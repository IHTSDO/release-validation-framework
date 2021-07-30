package org.ihtsdo.rvf.execution.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.execution.service.whitelist.WhitelistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MysqlFailuresExtractor {

    private final Logger logger = LoggerFactory.getLogger(MysqlFailuresExtractor.class);

    @Resource(name = "dataSource")
    private BasicDataSource dataSource;

    @Value("${rvf.qa.result.table.name}")
    private String qaResultTableName;

    @Autowired
    private WhitelistService whitelistService;

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
                item.setFailureCount(0L);
                item.setFirstNInstances(null);
            }
        }
    }

    private void validateFailuresAndExtractTestResults(Connection connection, List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<String, Integer> assertionIdToTotalFailureMap) throws SQLException, RestClientException {
        for (TestRunItem item : items) {
            String key = String.valueOf(uuidToAssertionIdMap.get(item.getAssertionUuid()));
            if (assertionIdToTotalFailureMap.containsKey(key)) {
                int batch_counter = 0;
                int total_failures_extracted = 0;
                int total_whitelistedItem_extracted = 0;
                int total_failures = assertionIdToTotalFailureMap.get(key);
                List<FailureDetail> firstNInstances = new ArrayList<>();
                while(batch_counter * config.getFailureExportMax() < total_failures && total_failures_extracted < config.getFailureExportMax()) {
                    int startIndex = batch_counter * config.getFailureExportMax();
                    int endIndex = startIndex + config.getFailureExportMax();
                    List<FailureDetail> failureDetails = fetchFailureDetails(connection, config.getExecutionId(), uuidToAssertionIdMap.get(item.getAssertionUuid()), -1, startIndex, endIndex);
                    failureDetails.stream().forEach(failureDetail -> {
                        failureDetail.setFullComponent(getAdditionalFields(connection,failureDetail));
                    });

                    // Convert to WhitelistItem
                    List<WhitelistItem> whitelistItems = failureDetails.stream()
                            .map(failureDetail -> new WhitelistItem(item.getAssertionUuid().toString(), StringUtils.isEmpty(failureDetail.getComponentId())? "" : failureDetail.getComponentId(), failureDetail.getConceptId(), failureDetail.getFullComponent()))
                            .collect(Collectors.toList());

                    // Send to Authoring acceptance gateway
                    List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);

                    // Find the failures which are not in the whitelisted item
                    List<FailureDetail> noneWhitelistedFailures = failureDetails.stream().filter(failureDetail ->
                        whitelistedItems.stream().noneMatch(whitelistedItem -> failureDetail.getComponentId().equals(whitelistedItem.getComponentId()))
                    ).collect(Collectors.toList());

                    total_whitelistedItem_extracted += whitelistedItems.size();
                    total_failures_extracted += noneWhitelistedFailures.size();
                    firstNInstances.addAll(noneWhitelistedFailures);
                    batch_counter++;
                }

                if (total_failures_extracted == 0) {
                    item.setFailureCount(0L);
                    item.setFirstNInstances(null);
                } else {
                    item.setFailureCount(Long.valueOf(total_failures - total_whitelistedItem_extracted));
                    item.setFirstNInstances(firstNInstances.size() > config.getFailureExportMax() ? firstNInstances.subList(0, config.getFailureExportMax() - 1) : firstNInstances);
                }
            } else {
                item.setFailureCount(0L);
                item.setFirstNInstances(null);
            }
        }
    }

    private String getAdditionalFields(Connection connection, FailureDetail failureDetail)  {
        if (StringUtils.isEmpty(failureDetail.getComponentId())) {
            return "";
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
                        if(additionalFields.length() > 0) {
                            additionalFields.append(",");
                        }
                        additionalFields.append(resultSet.getString(i));
                    }
                    return additionalFields.toString();
                }
            }
        } catch (SQLException exception) {
            logger.error("Error retrieving additional fields for component id {} against table {}", failureDetail.getComponentId(), failureDetail.getTableName());
        }

        return "";
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

    private List<FailureDetail> fetchFailureDetails(Connection connection, Long executionId, Long assertionId, int failureExportMax, Integer startIndex, Integer endIndex)
            throws SQLException {
        String resultSQL = "select concept_id, details, component_id, table_name from " + dataSource.getDefaultCatalog() + "." + qaResultTableName + " where assertion_id = ? and run_id = ?";
        if (startIndex != null && endIndex != null && startIndex > 0 && endIndex > 0) {
            resultSQL = resultSQL + " limit " + startIndex + "," + endIndex;
        }
        else if (failureExportMax > 0) {
            resultSQL = resultSQL + " limit ?";
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

}
