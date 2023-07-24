package org.ihtsdo.rvf.core.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.otf.rest.client.RestClientException;
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

import javax.annotation.Resource;
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

    @Resource(name = "dataSource")
    private BasicDataSource dataSource;

    @Value("${rvf.qa.result.table.name}")
    private String qaResultTableName;

    @Value("${rvf.assertion.whitelist.batchsize:1000}")
    private int whitelistBatchSize;

    @Autowired
    private WhitelistService whitelistService;

    @Autowired
    private AssertionService assertionService;

    public void extractTestResults(final List<TestRunItem> items, final MysqlExecutionConfig config, List<Assertion> assertions) throws SQLException, RestClientException {
        Map<UUID, Long> uuidToAssertionIdMap = assertions.stream().collect(Collectors.toMap(Assertion::getUuid, assertion -> assertion.getAssertionId()));
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Integer> assertionIdToTotalFailureMap = getAssertionIdToTotalFailureMap(connection, config);
            if (whitelistService.isWhitelistDisabled()) {
                extractTestResults(connection, items, config, uuidToAssertionIdMap, assertionIdToTotalFailureMap);
            } else {
                validateFailuresAndExtractTestResults(connection, items, config, uuidToAssertionIdMap, assertionIdToTotalFailureMap);
            }
        }
    }

    private void extractTestResults(Connection connection, List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<String, Integer> assertionIdToTotalFailureMap) throws SQLException {
        for (TestRunItem item : items) {
            String key = String.valueOf(uuidToAssertionIdMap.get(item.getAssertionUuid()));
            if (assertionIdToTotalFailureMap.containsKey(key)) {
                item.setFailureCount(Long.valueOf(assertionIdToTotalFailureMap.get(key)));
                item.setFirstNInstances(fetchFailureDetails(connection, config.getExecutionId(), uuidToAssertionIdMap.get(item.getAssertionUuid()), config.getFailureExportMax(), null, null));
            } else {
                if (StringUtils.isEmpty(item.getFailureMessage())) {
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
                        failureDetails = failureDetails.stream().filter(failure -> config.getIncludedModules().contains(failure.getModuleId())).collect(Collectors.toList());
                        totalFilteredOutFailures += (totalBatchFailures - failureDetails.size());
                    }

                    if (failureDetails.size() != 0) {
                        // Convert to WhitelistItem
                        List<WhitelistItem> whitelistItems = failureDetails.stream()
                                .map(failureDetail -> new WhitelistItem(item.getAssertionUuid().toString(), StringUtils.hasLength(failureDetail.getComponentId())? failureDetail.getComponentId() : "", failureDetail.getConceptId(), failureDetail.getFullComponent()))
                                .collect(Collectors.toList());

                        // Send to Authoring acceptance gateway
                        List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);

                        // Find the failures which are not in the whitelisted item
                        List<FailureDetail> validFailures = failureDetails.stream().filter(failure ->
                                whitelistedItems.stream().noneMatch(whitelistedItem -> failure.getComponentId().equals(whitelistedItem.getComponentId()))
                        ).collect(Collectors.toList());

                        totalWhitelistedFailures += whitelistedItems.size();
                        firstNInstances.addAll(validFailures);
                    }
                    batchCounter++;
                }

                if (firstNInstances.size() == 0) {
                    item.setFailureCount(0L);
                    item.setFirstNInstances(null);
                } else {
                    item.setFailureCount(Long.valueOf(totalFailures - totalWhitelistedFailures - totalFilteredOutFailures));
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
        String sql = "select * from " + failureDetail.getTableName()+ " where id = ?";
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
                        if(additionalFields.length() > 0) {
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

    private Map<String, Integer> getAssertionIdToTotalFailureMap(Connection connection, MysqlExecutionConfig config) throws SQLException {
        Map<String, Integer> assertionIdToTotalFailureMap = new HashMap<>();
        String totalSQL = "select assertion_id, count(*) total from " + dataSource.getDefaultCatalog() + "." + qaResultTableName + " where run_id = ? group by assertion_id";
        try (PreparedStatement preparedStatement = connection.prepareStatement(totalSQL)) {
            preparedStatement.setLong(1, config.getExecutionId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    assertionIdToTotalFailureMap.put(resultSet.getString(1), Integer.valueOf(resultSet.getInt(2)));
                }
            }
        }
        return assertionIdToTotalFailureMap;
    }

    private List<FailureDetail> fetchFailureDetails(Connection connection, Long executionId, Long assertionId, int failureExportMax, Integer offset, Integer rowCount)
            throws SQLException {
        String resultSQL = "select concept_id, details, component_id, table_name from " + dataSource.getDefaultCatalog() + "." + qaResultTableName + " where assertion_id = ? and run_id = ?";
        if (offset != null && rowCount != null) {
            resultSQL += " limit " + offset + "," + rowCount;
        }
        else if (failureExportMax > 0) {
            resultSQL += " limit ?";
        }
        List<FailureDetail> firstNInstances = new ArrayList();
        long counter = 0;
        try (PreparedStatement preparedStatement = connection.prepareStatement(resultSQL)) {
            // select results that match execution
            preparedStatement.setLong(1, assertionId);
            preparedStatement.setLong(2, executionId);
            if (failureExportMax > 0) {
                preparedStatement.setLong(3, failureExportMax);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    // only get first N failed results
                    if (failureExportMax < 0 || counter < failureExportMax) {
                        FailureDetail detail = new FailureDetail(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4));
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
        assertionGroups.stream().forEach(assertionGroup -> {
            assertionGroup.getAssertions().stream().forEach(a -> {
                assertions.stream().forEach(b -> {
                    if (a.getUuid().toString().equals(b.getUuid().toString())) {
                        b.addGroup(assertionGroup.getName());
                    }
                });
            });
        });
        return assertions;
    }

}
