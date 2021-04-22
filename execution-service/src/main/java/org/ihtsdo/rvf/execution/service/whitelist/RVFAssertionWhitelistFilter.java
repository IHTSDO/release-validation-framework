package org.ihtsdo.rvf.execution.service.whitelist;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.WhitelistService;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RVFAssertionWhitelistFilter {

    private final Logger logger = LoggerFactory.getLogger(RVFAssertionWhitelistFilter.class);

    @Resource(name = "dataSource")
    private BasicDataSource dataSource;

    @Value("${rvf.qa.result.table.name}")
    private String qaResultTableName;

    @Autowired
    private WhitelistService whitelistService;

    public void extractTestResults(final List<TestRunItem> items, final MysqlExecutionConfig config, List<Assertion> assertions) throws SQLException {
        Map<UUID, Long> uuidToAssertionIdMap = assertions.stream().collect(Collectors.toMap(Assertion::getUuid, assertion -> assertion.getAssertionId()));
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Integer> assertionIdToTotalFailureMap = getAssertionIdToTotalFailureMap(config, connection);
            if (whitelistService.isWhitelistDisabled()) {
                extractTestResults(items, config, uuidToAssertionIdMap, assertionIdToTotalFailureMap);
            } else {
                validateFailuresAndExtractTestResults(items, config, uuidToAssertionIdMap, assertionIdToTotalFailureMap);
            }
        }
    }

    private void extractTestResults(List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<String, Integer> assertionIdToTotalFailureMap) throws SQLException {
        for (TestRunItem item : items) {
            String key = String.valueOf(uuidToAssertionIdMap.get(item.getAssertionUuid()));
            if (assertionIdToTotalFailureMap.containsKey(key)) {
                item.setFailureCount(Long.valueOf(assertionIdToTotalFailureMap.get(key)));
                item.setFirstNInstances(fetchFailureDetails(config.getExecutionId(), uuidToAssertionIdMap.get(item.getAssertionUuid()), config.getFailureExportMax(), null, null));
            } else {
                item.setFailureCount(0L);
                item.setFirstNInstances(Collections.emptyList());
            }
        }
    }

    private void validateFailuresAndExtractTestResults(List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<String, Integer> assertionIdToTotalFailureMap) throws SQLException {
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
                    List<FailureDetail> failureDetails = fetchFailureDetails(config.getExecutionId(), uuidToAssertionIdMap.get(item.getAssertionUuid()), -1, startIndex, endIndex);

                    // Convert to WhitelistItem
                    List<WhitelistItem> whitelistItems = failureDetails.stream()
                            .map(failureDetail -> new WhitelistItem(item.getAssertionUuid().toString(), "", failureDetail.getConceptId(), ""))
                            .collect(Collectors.toList());

                    // Send to Authoring acceptance gateway
                    List<WhitelistItem> whitelistedItems = whitelistService.validateAssertions(whitelistItems);

                    // Find the failures which are not in the whitelisted item
                    List<FailureDetail> noneWhitelistedFailures = failureDetails.stream().filter(failureDetail ->
                        whitelistedItems.stream().noneMatch(whitelistedItem -> failureDetail.getConceptId().equals(whitelistedItem.getConceptId()))
                    ).collect(Collectors.toList());

                    total_whitelistedItem_extracted += whitelistedItems.size();
                    total_failures_extracted += noneWhitelistedFailures.size();
                    firstNInstances.addAll(noneWhitelistedFailures);
                    batch_counter++;
                }

                if (total_failures_extracted == 0) {
                    item.setFailureCount(0L);
                    item.setFirstNInstances(Collections.emptyList());
                } else {
                    item.setFailureCount(Long.valueOf(total_failures - total_whitelistedItem_extracted));
                    item.setFirstNInstances(firstNInstances.size() > config.getFailureExportMax() ? firstNInstances.subList(0, config.getFailureExportMax() - 1) : firstNInstances);
                }
            } else {
                item.setFailureCount(0L);
                item.setFirstNInstances(Collections.emptyList());
            }
        }
    }

    private Map<String, Integer> getAssertionIdToTotalFailureMap(MysqlExecutionConfig config, Connection connection) throws SQLException {
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

    private List<FailureDetail> fetchFailureDetails(Long executionId, Long assertionId, int failureExportMax, Integer startIndex, Integer endIndex)
            throws SQLException {
		/*
		 create a prepared statement for retrieving matching results.
		*/
        String resultSQL = "select concept_id, details from " + dataSource.getDefaultCatalog() + "." + qaResultTableName + " where assertion_id = ? and run_id = ?";
        //use limit to save memory and improve performance for worst case when containing thousands of errors
        if (startIndex != null && endIndex != null && startIndex > 0 && endIndex > 0) {
            resultSQL = resultSQL + " limit " + startIndex + "," + endIndex;
        }
        else if (failureExportMax > 0) {
            resultSQL = resultSQL + " limit ?";
        }
        List<FailureDetail> firstNInstances = new ArrayList();
        try (Connection connection = dataSource.getConnection()) {
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
                            FailureDetail detail = new FailureDetail(resultSet.getString(1), resultSet.getString(2));
                            firstNInstances.add(detail);
                        }
                        counter++;
                    }
                }
            }
        }
        return firstNInstances;
    }

}
