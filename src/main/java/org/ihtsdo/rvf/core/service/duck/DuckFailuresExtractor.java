package org.ihtsdo.rvf.core.service.duck;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.utils.SnomedUtilsBase;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.FailureDetail;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.WhitelistService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.whitelist.WhitelistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads qa_result back to fill in each {@link TestRunItem}'s failure count and sample
 * failures: the DuckDB counterpart of {@code MysqlFailuresExtractor}, with the same
 * classification, whitelist filtering, extension-module filtering and FSN lookup.
 *
 * <p>The differences are those of the engine:
 * <ul>
 * <li>It works on the run's open DuckDB {@link Connection} and a schema-qualified
 *     qa_result table, rather than a pooled datasource and its default catalog.
 * <li>qa_result.assertion_id holds the assertion UUID (see
 *     {@link DuckDbAssertionExecutionService}), so no uuid-to-numeric-id map is needed.
 * <li>Paging is DuckDB's {@code limit ? offset ?}: count first, then offset, the reverse
 *     of MySQL's {@code limit ?,?}.
 * </ul>
 */
public class DuckFailuresExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckFailuresExtractor.class);

	private static final int MAX_SCTID_LENGTH = 18;

	/**
	 * A schema-qualified SQL identifier. Every identifier this class interpolates is chosen
	 * by the engine or the corpus, so anything else is refused rather than escaped.
	 */
	private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*(\\.[A-Za-z_]\\w*)*");

	private final Connection connection;
	private final String qaResultTable;
	private final WhitelistService whitelistService;
	private final Supplier<List<Assertion>> assertionsWithGroups;
	private final int whitelistBatchSize;
	private final int fsnLookupBatchSize;
	private final List<String> fsnLookupSchemas;

	/**
	 * @param assertionsWithGroups the run's assertions with their group names, which decide
	 *                             whether module filtering applies
	 * @param fsnLookupSchemas     schemas to read concept FSNs from, in order of preference
	 */
	public DuckFailuresExtractor(Connection connection, String qaResultTable, WhitelistService whitelistService,
			Supplier<List<Assertion>> assertionsWithGroups, int whitelistBatchSize, int fsnLookupBatchSize,
			List<String> fsnLookupSchemas) {
		this.connection = connection;
		this.qaResultTable = requireIdentifier(qaResultTable, "qaResultTable");
		this.whitelistService = whitelistService;
		this.assertionsWithGroups = assertionsWithGroups;
		this.whitelistBatchSize = whitelistBatchSize;
		this.fsnLookupBatchSize = Math.max(1, fsnLookupBatchSize);
		fsnLookupSchemas.forEach(schema -> requireIdentifier(schema, "fsnLookupSchema"));
		this.fsnLookupSchemas = List.copyOf(fsnLookupSchemas);
	}

	private static String requireIdentifier(String value, String what) {
		if (value == null || !IDENTIFIER.matcher(value).matches()) {
			throw new IllegalArgumentException(what + " is not a plain SQL identifier: " + value);
		}
		return value;
	}

	/** Fills in {@code failureCount} and {@code firstNInstances} on every item. */
	public void extractTestResults(List<TestRunItem> items, MysqlExecutionConfig config) throws SQLException, RestClientException {
		Map<String, Long> totals = failureTotals(config.getExecutionId());
		if (whitelistService.isWhitelistDisabled()) {
			extractTestResults(items, totals, config);
		} else {
			validateFailuresAndExtractTestResults(items, config, totals);
		}
		populateConceptFsns(items);
	}

	private void extractTestResults(List<TestRunItem> items, Map<String, Long> totals, MysqlExecutionConfig config)
			throws SQLException {
		for (TestRunItem item : items) {
			String key = item.getAssertionUuid().toString();
			if (totals.containsKey(key)) {
				item.setFailureCount(totals.get(key));
				item.setFirstNInstances(fetchFailureDetails(config.getExecutionId(), key, config.getFailureExportMax(), null, null));
			} else if (!StringUtils.hasLength(item.getFailureMessage())) {
				item.setFailureCount(0L);
				item.setFirstNInstances(null);
			} else {
				item.setFailureCount(-1L);
				item.setFirstNInstances(null);
			}
		}
	}

	private void validateFailuresAndExtractTestResults(List<TestRunItem> items, MysqlExecutionConfig config,
			Map<String, Long> totals) throws SQLException, RestClientException {
		Map<UUID, Assertion> byUuid = assertionsWithGroups.get().stream()
				.collect(Collectors.toMap(Assertion::getUuid, Function.identity()));

		for (TestRunItem item : items) {
			UUID assertionUuid = item.getAssertionUuid();
			String key = assertionUuid.toString();
			if (StringUtils.hasLength(item.getFailureMessage())) {
				item.setFailureCount(-1L);
				item.setFirstNInstances(null);
			} else if (totals.containsKey(key)) {
				int batchCounter = 0;
				int totalWhitelistedFailures = 0;
				int totalFilteredOutFailures = 0;
				long totalFailures = totals.get(key);
				Assertion assertion = byUuid.get(assertionUuid);
				boolean commonAuthoringOrEdition = assertion != null && assertion.getGroups() != null
						&& (assertion.getGroups().contains("common-edition") || assertion.getGroups().contains("common-authoring"));
				List<FailureDetail> firstNInstances = new ArrayList<>();
				while (batchCounter * whitelistBatchSize < totalFailures && firstNInstances.size() < config.getFailureExportMax()) {
					int offset = batchCounter * whitelistBatchSize;
					List<FailureDetail> failureDetails = fetchFailureDetails(config.getExecutionId(), key, -1, offset, whitelistBatchSize);
					failureDetails.forEach(this::setModuleAndFullFields);

					// filter by the extension modules only
					if (commonAuthoringOrEdition && config.isExtensionValidation() && !CollectionUtils.isEmpty(config.getIncludedModules())) {
						int totalBatchFailures = failureDetails.size();
						failureDetails = failureDetails.stream().filter(failure -> Boolean.TRUE.equals(failure.getSkipModuleCheck())
								|| config.getIncludedModules().contains(failure.getModuleId())).toList();
						totalFilteredOutFailures += (totalBatchFailures - failureDetails.size());
					}

					if (!failureDetails.isEmpty()) {
						List<WhitelistItem> whitelistItems = failureDetails.stream()
								.map(failureDetail -> new WhitelistItem(key, StringUtils.hasLength(failureDetail.getComponentId()) ? failureDetail.getComponentId() : "",
										failureDetail.getConceptId(), failureDetail.getFullComponent()))
								.toList();
						List<WhitelistItem> whitelistedItems = whitelistService.checkComponentFailuresAgainstWhitelist(whitelistItems);
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
					item.setFailureCount(totalFailures - totalWhitelistedFailures - totalFilteredOutFailures);
					item.setFirstNInstances(firstNInstances.size() > config.getFailureExportMax()
							? firstNInstances.subList(0, config.getFailureExportMax()) : firstNInstances);
				}
			} else {
				item.setFailureCount(0L);
				item.setFirstNInstances(null);
			}
		}
	}

	/**
	 * Adds the component's module id and its remaining fields, as the MySQL extractor does,
	 * by column position: the materialiser creates every table with the DDL's column order.
	 *
	 * <p>{@code table_name} comes from the assertion's SQL and is concatenated into the query,
	 * so it is checked against the catalog first.
	 */
	private void setModuleAndFullFields(final FailureDetail failureDetail) {
		if (!StringUtils.hasLength(failureDetail.getComponentId())) {
			return;
		}
		String table = failureDetail.getTableName();
		if (!isKnownTable(table)) {
			LOGGER.warn("Not enriching failure detail: table_name {} is not a table in this run's database", table);
			return;
		}
		String sql = "select * from " + table + " where id = ?";
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
			LOGGER.error("Error retrieving additional fields for component id {} against table {}",
					failureDetail.getComponentId(), failureDetail.getTableName());
		}
	}

	private boolean isKnownTable(String table) {
		if (table == null || !IDENTIFIER.matcher(table).matches()) {
			return false;
		}
		int dot = table.lastIndexOf('.');
		String schema = dot < 0 ? null : table.substring(0, dot);
		String name = dot < 0 ? table : table.substring(dot + 1);
		String sql = "select 1 from information_schema.tables where lower(table_name) = lower(?)"
				+ (schema == null ? "" : " and lower(table_schema) = lower(?)");
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, name);
			if (schema != null) {
				ps.setString(2, schema);
			}
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			LOGGER.warn("Could not verify table {} against the catalog", table, e);
			return false;
		}
	}

	/** The concept FSN for every reported failure that names a concept, as the MySQL extractor adds. */
	private void populateConceptFsns(List<TestRunItem> items) {
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
		for (String schema : fsnLookupSchemas) {
			if (failuresByConceptId.isEmpty()) {
				return;
			}
			List<String> conceptIds = new ArrayList<>(failuresByConceptId.keySet());
			for (int i = 0; i < conceptIds.size(); i += fsnLookupBatchSize) {
				List<String> batch = conceptIds.subList(i, Math.min(i + fsnLookupBatchSize, conceptIds.size()));
				fetchConceptFsns(schema, batch).forEach((conceptId, fsn) -> {
					List<FailureDetail> failures = failuresByConceptId.remove(conceptId);
					if (failures != null) {
						failures.forEach(failure -> failure.setConceptFsn(fsn));
					}
				});
			}
		}
	}

	private static boolean needsConceptFsnLookup(FailureDetail failure) {
		String conceptId = failure.getConceptId();
		return !StringUtils.hasLength(failure.getConceptFsn())
				&& SnomedUtilsBase.isSctid(conceptId)
				&& conceptId.length() <= MAX_SCTID_LENGTH
				&& SnomedUtilsBase.isConceptSctid(conceptId);
	}

	private Map<String, String> fetchConceptFsns(String schema, List<String> conceptIds) {
		Map<String, String> conceptIdToFsn = new HashMap<>();
		// Prefer the active, English, most recent FSN when a concept has several
		String sql = "select conceptid, term from " + schema + ".description_s"
				+ " where typeid = " + RF2Constants.SCTID_FSN
				+ " and conceptid in (" + String.join(",", Collections.nCopies(conceptIds.size(), "?")) + ")"
				+ " order by active desc, languagecode = 'en' desc, effectivetime desc";
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
			LOGGER.error("Error retrieving concept FSNs from {}", schema, exception);
		}
		return conceptIdToFsn;
	}

	private Map<String, Long> failureTotals(Long executionId) throws SQLException {
		Map<String, Long> totals = new HashMap<>();
		String sql = "select assertion_id, count(*) total from " + qaResultTable + " where run_id = ? group by assertion_id";
		try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setLong(1, executionId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					totals.put(resultSet.getString(1), resultSet.getLong(2));
				}
			}
		}
		return totals;
	}

	private List<FailureDetail> fetchFailureDetails(Long executionId, String assertionUuid, int failureExportMax,
			Integer offset, Integer rowCount) throws SQLException {
		String sql = "select concept_id, details, component_id, table_name, skip_module_check from " + qaResultTable
				+ " where assertion_id = ? and run_id = ?";
		if (offset != null && rowCount != null) {
			sql += " limit ? offset ?";
		} else if (failureExportMax > 0) {
			sql += " limit ?";
		}
		List<FailureDetail> firstNInstances = new ArrayList<>();
		long counter = 0;
		try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, assertionUuid);
			preparedStatement.setLong(2, executionId);
			if (offset != null && rowCount != null) {
				// limit (row count) first, then offset
				preparedStatement.setInt(3, rowCount);
				preparedStatement.setInt(4, offset);
			} else if (failureExportMax > 0) {
				preparedStatement.setLong(3, failureExportMax);
			}
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					if (failureExportMax < 0 || counter < failureExportMax) {
						firstNInstances.add(new FailureDetail(resultSet.getString(1), resultSet.getString(2),
								resultSet.getString(3), resultSet.getString(4), resultSet.getBoolean(5)));
					}
					counter++;
				}
			}
		}
		return firstNInstances;
	}
}
