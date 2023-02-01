package org.ihtsdo.rvf.execution.service;

import com.google.common.collect.Iterables;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
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

	public void extractTestResults(final List<TestRunItem> items, final MysqlExecutionConfig config, List<Assertion> assertions) throws SQLException, RestClientException {
		Map<UUID, Long> uuidToAssertionIdMap = assertions.stream().collect(Collectors.toMap(Assertion::getUuid, assertion -> assertion.getAssertionId()));
		try (Connection connection = dataSource.getConnection()) {
			List<FailureDetail> failureDetails = getAllFailures(connection, config.getExecutionId());

			if (config.isExcludeDependencyFailures()) {
				String dependencySchema = ValidationVersionLoader.constructRVFSchema(config.getExtensionDependencyVersion());
				if (dependencySchema != null) {
					failureDetails = excludeDependencyFailures(connection, failureDetails, dependencySchema);
				}
			}

			Map<Long, List<FailureDetail>> assertionIdToFailuresMap = failureDetails.stream().collect(Collectors.groupingBy(FailureDetail::getAssertionId, HashMap::new, Collectors.toCollection(ArrayList::new)));
			if (whitelistService.isWhitelistDisabled()) {
				extractTestResults(connection, items, config, uuidToAssertionIdMap, assertionIdToFailuresMap);
			} else {
				validateFailuresAndExtractTestResults(connection, items, config, uuidToAssertionIdMap, assertionIdToFailuresMap);
			}
		}
	}

	private List<FailureDetail> excludeDependencyFailures(Connection connection, List<FailureDetail> failureDetails, String dependencySchema) throws SQLException {
		List<FailureDetail> result = new ArrayList<>();
		Map<String, List<FailureDetail>> tableToFailureMap = failureDetails.stream().collect(Collectors.groupingBy(FailureDetail::getTableName, HashMap::new, Collectors.toCollection(ArrayList::new)));
		for (String tableName : tableToFailureMap.keySet()) {
			List<String> dependencyComponentIds = new ArrayList();
			List<String> componentIds = tableToFailureMap.get(tableName).stream().map(FailureDetail::getComponentId).collect(Collectors.toList());
			for (List<String> batch : Iterables.partition(componentIds, 100)) {
				String resultSQL = "select id from " + dependencySchema + "." + (tableName.contains(".") ? tableName.substring(tableName.lastIndexOf(".") + 1) : tableName) + " where id in (" + org.apache.commons.lang3.StringUtils.join(batch, ',') + ")";
				try (PreparedStatement preparedStatement = connection.prepareStatement(resultSQL)) {
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						while (resultSet.next()) {
							dependencyComponentIds.add(resultSet.getString(1));
						}
					}
				}
			}

			result.addAll(tableToFailureMap.get(tableName).stream().filter(failureDetail -> !dependencyComponentIds.contains(failureDetail.getComponentId())).collect(Collectors.toList()));
		}

		return result;
	}

	private void extractTestResults(Connection connection, List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<Long, List<FailureDetail>> assertionIdToFailuresMap) throws SQLException {
		for (TestRunItem item : items) {
			String key = String.valueOf(uuidToAssertionIdMap.get(item.getAssertionUuid()));
			if (assertionIdToFailuresMap.containsKey(key)) {
				List<FailureDetail> failures = assertionIdToFailuresMap.get(key);
				item.setFailureCount(Long.valueOf(failures.size()));
				item.setFirstNInstances(failures.size() > config.getFailureExportMax() ? failures.subList(0, config.getFailureExportMax()) : failures);
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

	private void validateFailuresAndExtractTestResults(Connection connection, List<TestRunItem> items, MysqlExecutionConfig config, Map<UUID, Long> uuidToAssertionIdMap, Map<Long, List<FailureDetail>> assertionIdToFailuresMap) throws SQLException, RestClientException {
		for (TestRunItem item : items) {
			String assertionId = String.valueOf(uuidToAssertionIdMap.get(item.getAssertionUuid()));
			if (!StringUtils.isEmpty(item.getFailureMessage())) {
				item.setFailureCount(-1L);
				item.setFirstNInstances(null);
			} else if (assertionIdToFailuresMap.containsKey(assertionId)) {
				List<FailureDetail> allFailures = assertionIdToFailuresMap.get(assertionId);
				int totalFailures = 0;
				int totalWhitelistedItems = 0;
				List<FailureDetail> firstNInstances = new ArrayList<>();
				for (List<FailureDetail> failures : Iterables.partition(allFailures, whitelistBatchSize)) {
					failures.stream().forEach(failure -> {
						failure.setFullComponent(getAdditionalFields(connection, failure));
					});

					// Convert to WhitelistItem
					List<WhitelistItem> whitelistItems = failures.stream()
							.map(failure -> new WhitelistItem(item.getAssertionUuid().toString(),
															StringUtils.isEmpty(failure.getComponentId()) ? "" : failure.getComponentId(),
															failure.getConceptId(),
															failure.getFullComponent()))
							.collect(Collectors.toList());

					// Send to Authoring Acceptance Gateway
					List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);

					// Find the failures which are not in the whitelisted item
					List<FailureDetail> validFailures = failures.stream().filter(failure ->
							whitelistedItems.stream().noneMatch(whitelistedItem -> failure.getComponentId().equals(whitelistedItem.getComponentId()))
					).collect(Collectors.toList());

					totalWhitelistedItems += whitelistedItems.size();
					totalFailures += validFailures.size();
					firstNInstances.addAll(validFailures);
					if (totalFailures >=  config.getFailureExportMax()) {
						break;
					}
				}
				if (totalFailures == 0) {
					item.setFailureCount(0L);
					item.setFirstNInstances(null);
				} else {
					item.setFailureCount(Long.valueOf(allFailures.size() - totalWhitelistedItems));
					item.setFirstNInstances(firstNInstances.size() > config.getFailureExportMax() ? firstNInstances.subList(0, config.getFailureExportMax()) : firstNInstances);
				}
			} else {
				item.setFailureCount(0L);
				item.setFirstNInstances(null);
			}
		}
	}

	private String getAdditionalFields(Connection connection, FailureDetail failureDetail) {
		if (StringUtils.isEmpty(failureDetail.getComponentId())) {
			return "";
		}
		String sql = "select * from " + failureDetail.getTableName() + " where id = ?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, failureDetail.getComponentId());
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					int columnCount = resultSet.getMetaData().getColumnCount();
					StringBuilder additionalFields = new StringBuilder();
					for (int i = 1; i <= columnCount; i++) {
						// Ignore columns: id and effective time
						if (i == 1 || i == 2) {
							continue;
						}
						if (additionalFields.length() > 0) {
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

	private List<FailureDetail> getAllFailures(Connection connection, Long executionId) throws SQLException {
		String resultSQL = "select concept_id, details, component_id, table_name, assertion_id from " + dataSource.getDefaultCatalog() + "." + qaResultTableName + " where run_id = ?";
		List<FailureDetail> failureInstances = new ArrayList();
		try (PreparedStatement preparedStatement = connection.prepareStatement(resultSQL)) {
			preparedStatement.setLong(1, executionId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					FailureDetail detail = new FailureDetail(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getLong(5));
					failureInstances.add(detail);
				}
			}
		}
		return failureInstances;
	}
}
