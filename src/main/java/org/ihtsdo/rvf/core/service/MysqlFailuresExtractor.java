package org.ihtsdo.rvf.core.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.utils.SnomedUtilsBase;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.ihtsdo.rvf.core.data.model.FailureDetail;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.whitelist.WhitelistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MysqlFailuresExtractor {

    private final Logger logger = LoggerFactory.getLogger(MysqlFailuresExtractor.class);

    private static final int MAX_SCTID_LENGTH = 18;

    @Resource(name = "dataSource")
    private BasicDataSource dataSource;

    @Value("${rvf.qa.result.table.name}")
    private String qaResultTableName;

    @Value("${rvf.assertion.whitelist.batchsize:1000}")
    private int whitelistBatchSize;

    @Value("${rvf.validation.fsn-lookup-batch-size:1000}")
    private int fsnLookupBatchSize;

    @Autowired
    private WhitelistService whitelistService;

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private ReleaseDataManager releaseDataManager;

    public void extractTestResults(final List<TestRunItem> items, final MysqlExecutionConfig config, List<Assertion> assertions) throws SQLException, RestClientException {
        Map<UUID, Long> uuidToAssertionIdMap = assertions.stream().collect(Collectors.toMap(Assertion::getUuid, Assertion::getAssertionId));
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Integer> assertionIdToTotalFailureMap = getAssertionIdToTotalFailureMap(connection, config);
            if (whitelistService.isWhitelistDisabled()) {
                extractTestResults(connection, items, config, uuidToAssertionIdMap, assertionIdToTotalFailureMap);
            } else {
                validateFailuresAndExtractTestResults(connection, items, config, uuidToAssertionIdMap, assertionIdToTotalFailureMap);
            }
            populateConceptFsns(connection, items, config);
        }
    }

    private void extractTestResults(Connection connection, List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<String, Integer> assertionIdToTotalFailureMap) throws SQLException {
        for (TestRunItem item : items) {
            String key = String.valueOf(uuidToAssertionIdMap.get(item.getAssertionUuid()));
            if (assertionIdToTotalFailureMap.containsKey(key)) {
                item.setFailureCount(Long.valueOf(assertionIdToTotalFailureMap.get(key)));
                item.setFirstNInstances(fetchFailureDetails(connection, config.getExecutionId(), uuidToAssertionIdMap.get(item.getAssertionUuid()), config.getFailureExportMax(), null, null));
            } else {
                if (!StringUtils.hasLength(item.getFailureMessage())) {
                    item.setFailureCount(0L);
                    item.setFirstNInstances(null);
                } else {
                    item.setFailureCount(-1L);
                    item.setFirstNInstances(null);
                }
            }
        }
    }

    private void validateFailuresAndExtractTestResults(Connection connection, List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<String, Integer> assertionIdToTotalFailureMap) throws SQLException, RestClientException {
        List<Assertion> assertions = getAssertionsAndJoinGroups();
        Map<UUID, Assertion> uuidAssertionMap = assertions.stream().collect(Collectors.toMap(Assertion::getUuid, Function.identity()));

        for (TestRunItem item : items) {
            UUID assertionUuid = item.getAssertionUuid();
            String key = String.valueOf(uuidToAssertionIdMap.get(assertionUuid));
            if (StringUtils.hasLength(item.getFailureMessage())) {
                item.setFailureCount(-1L);
                item.setFirstNInstances(null);
            } else if (assertionIdToTotalFailureMap.containsKey(key)) {
                int batchCounter = 0;
                int totalWhitelistedFailures = 0;
                int totalFilteredOutFailures = 0;
                int totalFailures = assertionIdToTotalFailureMap.get(key);
                boolean belongToCommonAuthoringOrCommonEditionGroup = uuidAssertionMap.containsKey(assertionUuid) && uuidAssertionMap.get(assertionUuid).getGroups() != null
                                                                    && (uuidAssertionMap.get(assertionUuid).getGroups().contains("common-edition") || uuidAssertionMap.get(assertionUuid).getGroups().contains("common-authoring"));
                List<FailureDetail> firstNInstances = new ArrayList<>();
                while(batchCounter * whitelistBatchSize < totalFailures && firstNInstances.size() < config.getFailureExportMax()) {
                    int offset = batchCounter * whitelistBatchSize;
                    List<FailureDetail> failureDetails = fetchFailureDetails(connection, config.getExecutionId(), uuidToAssertionIdMap.get(item.getAssertionUuid()), -1, offset, whitelistBatchSize);
                    failureDetails.forEach(failure -> setModuleAndFullFields(connection, failure));

                    // filter by the extension modules only
                    if (belongToCommonAuthoringOrCommonEditionGroup && config.isExtensionValidation() && !CollectionUtils.isEmpty(config.getIncludedModules())) {
                        int totalBatchFailures = failureDetails.size();
                        failureDetails = failureDetails.stream().filter(failure -> Boolean.TRUE.equals(failure.getSkipModuleCheck()) || config.getIncludedModules().contains(failure.getModuleId())).toList();
                        totalFilteredOutFailures += (totalBatchFailures - failureDetails.size());
                    }

                    if (!failureDetails.isEmpty()) {
                        // Convert to WhitelistItem
                        List<WhitelistItem> whitelistItems = failureDetails.stream()
                                .map(failureDetail -> new WhitelistItem(item.getAssertionUuid().toString(), StringUtils.hasLength(failureDetail.getComponentId())? failureDetail.getComponentId() : "", failureDetail.getConceptId(), failureDetail.getFullComponent()))
                                .toList();

                        // Send to Authoring acceptance gateway
                        List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);

                        // Find the failures which are not in the whitelisted item
                        List<FailureDetail> validFailures = failureDetails.stream().filter(failure ->
                                whitelistedItems.stream().noneMatch(whitelistedItem -> failure.getComponentId().equals(whitelistedItem.getComponentId()))
                        ).toList();

                        totalWhitelistedFailures += whitelistedItems.size();
                        firstNInstances.addAll(validFailures);
                    }
                    batchCounter++;
                }

                if (firstNInstances.isEmpty()) {
                    item.setFailureCount(0L);
                    item.setFirstNInstances(null);
                } else {
                    item.setFailureCount((long) (totalFailures - totalWhitelistedFailures - totalFilteredOutFailures));
                    item.setFirstNInstances(firstNInstances.size() > config.getFailureExportMax() ? firstNInstances.subList(0, config.getFailureExportMax()) : firstNInstances);
                }
            } else {
                item.setFailureCount(0L);
                item.setFirstNInstances(null);
            }
        }
    }

    private void setModuleAndFullFields(Connection connection, final FailureDetail failureDetail)  {
        if (!StringUtils.hasLength(failureDetail.getComponentId())) {
            return;
        }
        String sql = "select * from " + failureDetail.getTableName() + " where id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, failureDetail.getComponentId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int columnCount = resultSet.getMetaData().getColumnCount();
                    StringBuilder additionalFields = new StringBuilder();
                    for (int i =1; i <= columnCount; i ++) {
                        // Ignore columns: id and effective time
                        if (i == 1 || i == 2) {
                            continue;
                        }
                        // Column moduleId
                        if (i == 4) {
                            failureDetail.setModuleId(resultSet.getString(i));
                        }
                        if (!additionalFields.isEmpty()) {
                            additionalFields.append(",");
                        }
                        additionalFields.append(resultSet.getString(i));
                    }
                    failureDetail.setFullComponent(additionalFields.toString());
                }
            }
        } catch (SQLException exception) {
            logger.error("Error retrieving additional fields for component id {} against table {}", failureDetail.getComponentId(), failureDetail.getTableName());
        }
    }

    private void populateConceptFsns(Connection connection, List<TestRunItem> items, MysqlExecutionConfig config) {
        Map<String, List<FailureDetail>> failuresByConceptId = groupFailuresMissingFsnByConceptId(items);
        int batchSize = Math.max(1, fsnLookupBatchSize);
        for (String schema : getFsnLookupSchemas(config)) {
            if (failuresByConceptId.isEmpty()) {
                return;
            }
            List<String> conceptIds = new ArrayList<>(failuresByConceptId.keySet());
            for (int i = 0; i < conceptIds.size(); i += batchSize) {
                List<String> batch = conceptIds.subList(i, Math.min(i + batchSize, conceptIds.size()));
                fetchConceptFsns(connection, schema, batch).forEach((conceptId, fsn) -> {
                    List<FailureDetail> failures = failuresByConceptId.remove(conceptId);
                    if (failures != null) {
                        failures.forEach(failure -> failure.setConceptFsn(fsn));
                    }
                });
            }
        }
    }

    private Map<String, List<FailureDetail>> groupFailuresMissingFsnByConceptId(List<TestRunItem> items) {
        Map<String, List<FailureDetail>> failuresByConceptId = new HashMap<>();
        for (TestRunItem item : items) {
            if (item.getFirstNInstances() == null) {
                continue;
            }
            for (FailureDetail failure : item.getFirstNInstances()) {
                if (needsConceptFsnLookup(failure)) {
                    failuresByConceptId.computeIfAbsent(failure.getConceptId(), k -> new ArrayList<>()).add(failure);
                }
            }
        }
        return failuresByConceptId;
    }

    private boolean needsConceptFsnLookup(FailureDetail failure) {
        String conceptId = failure.getConceptId();
        return !StringUtils.hasLength(failure.getConceptFsn())
                && SnomedUtilsBase.isSctid(conceptId)
                && conceptId.length() <= MAX_SCTID_LENGTH
                && SnomedUtilsBase.isConceptSctid(conceptId);
    }

    private Set<String> getFsnLookupSchemas(MysqlExecutionConfig config) {
        Set<String> schemas = new LinkedHashSet<>();
        for (String schema : Arrays.asList(config.getProspectiveVersion(), config.getExtensionDependencyVersion(), config.getPreviousVersion())) {
            if (releaseDataManager.isKnownRelease(schema)) {
                schemas.add(schema);
            }
        }
        return schemas;
    }

    private Map<String, String> fetchConceptFsns(Connection connection, String schema, List<String> conceptIds) {
        Map<String, String> conceptIdToFsn = new HashMap<>();
        // Prefer the active, English, most recent FSN when a concept has several
        String sql = "select conceptid, term from " + schema + ".description_s" +
                " where typeid = " + RF2Constants.SCTID_FSN +
                " and conceptid in (" + String.join(",", Collections.nCopies(conceptIds.size(), "?")) + ")" +
                " order by active desc, languagecode = 'en' desc, effectivetime desc";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < conceptIds.size(); i++) {
                preparedStatement.setLong(i + 1, Long.parseLong(conceptIds.get(i)));
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    conceptIdToFsn.putIfAbsent(resultSet.getString(1), resultSet.getString(2));
                }
            }
        } catch (SQLException exception) {
            logger.error("Error retrieving concept FSNs from {}", schema, exception);
        }
        return conceptIdToFsn;
    }

    private Map<String, Integer> getAssertionIdToTotalFailureMap(Connection connection, MysqlExecutionConfig config) throws SQLException {
        Map<String, Integer> assertionIdToTotalFailureMap = new HashMap<>();
        String totalSQL = "select assertion_id, count(*) total from " + dataSource.getDefaultCatalog() + "." + qaResultTableName +
                " where run_id = ? group by assertion_id";
        try (PreparedStatement preparedStatement = connection.prepareStatement(totalSQL)) {
            preparedStatement.setLong(1, config.getExecutionId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    assertionIdToTotalFailureMap.put(resultSet.getString(1), resultSet.getInt(2));
                }
            }
        }
        return assertionIdToTotalFailureMap;
    }

    private List<FailureDetail> fetchFailureDetails(Connection connection, Long executionId, Long assertionId, int failureExportMax, Integer offset, Integer rowCount)
            throws SQLException {
        String resultSQL = "select concept_id, details, component_id, table_name, skip_module_check from " + dataSource.getDefaultCatalog() + "." + qaResultTableName +
                " where assertion_id = ? and run_id = ?";
        if (offset != null && rowCount != null) {
            resultSQL += " limit ?,?";
        } else if (failureExportMax > 0) {
            resultSQL += " limit ?";
        }
        List<FailureDetail> firstNInstances = new ArrayList<>();
        long counter = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement(resultSQL)) {
            // select results that match execution
            preparedStatement.setLong(1, assertionId);
            preparedStatement.setLong(2, executionId);
            if (offset != null && rowCount != null) {
                preparedStatement.setInt(3, offset);
                preparedStatement.setInt(4, rowCount);
            } else if (failureExportMax > 0) {
                preparedStatement.setLong(3, failureExportMax);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    // only get first N failed results
                    if (failureExportMax < 0 || counter < failureExportMax) {
                        FailureDetail detail = new FailureDetail(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getBoolean(5));
                        firstNInstances.add(detail);
                    }
                    counter++;
                }
            }
        }
        return firstNInstances;
    }

    private List<Assertion> getAssertionsAndJoinGroups() {
        List<Assertion> assertions = assertionService.findAll();
        List<AssertionGroup> assertionGroups = assertionService.getAllAssertionGroups();
        assertionGroups.forEach(assertionGroup -> assertionGroup.getAssertions().forEach(a -> assertions.forEach(b -> {
            if (a.getUuid().toString().equals(b.getUuid().toString())) {
                b.addGroup(assertionGroup.getName());
            }
        })));
        return assertions;
    }

}
