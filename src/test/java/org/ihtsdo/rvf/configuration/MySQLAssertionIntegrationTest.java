package org.ihtsdo.rvf.configuration;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.service.AssertionExecutionService;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.service.MysqlFailuresExtractor;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.ihtsdo.rvf.core.service.RvfDynamicDataSource;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public abstract class MySQLAssertionIntegrationTest extends IntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MySQLAssertionIntegrationTest.class);
	private static final AtomicLong RUN_ID = new AtomicLong(1L);
	private final Set<String> codeSystemVersions = new LinkedHashSet<>();
	private final Set<Long> usedRunIds = new LinkedHashSet<>();

	@Value("${rvf.master.schema.name}")
	private String masterSchemaName;

	@Autowired
	private AssertionExecutionService assertionExecutionService;

	@Autowired
	private AssertionService assertionService;

	@Autowired
	private MysqlFailuresExtractor mysqlFailuresExtractor;

	@Autowired
	private ReleaseDataManager releaseDataManager;

	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;

	@AfterEach
	protected void afterEach() {
		dropCodeSystemVersions();
		emptyQAResults();
	}

	private void dropCodeSystemVersions() {
		codeSystemVersions.forEach(this::dropCodeSystemVersion);
		codeSystemVersions.clear();
	}

	private void emptyQAResults() {
		if (!usedRunIds.isEmpty()) {
			try (Connection connection = rvfDynamicDataSource.getConnection(masterSchemaName);
				 PreparedStatement statement = connection.prepareStatement("DELETE FROM qa_result WHERE run_id = ?")) {
				for (Long id : usedRunIds) {
					statement.setLong(1, id);
					statement.execute();
				}
			} catch (Exception e) {
				LOGGER.error("Failed to clean up qa_result for run IDs {}", usedRunIds, e);
			}
			usedRunIds.clear();
		}
	}

	protected String createCodeSystemVersion(String codeSystemVersion) {
		try {
			String schemaVersion = codeSystemVersion.toLowerCase(Locale.ROOT)
					.replace("/", "_")
					.replace("-", "");
			String schemaName = releaseDataManager.createSchema(schemaVersion);
			codeSystemVersions.add(schemaName);
			return schemaName;
		} catch (Exception e) {
			LOGGER.error("9fc7f95d-bf0a-400d-825d-1716853d4432 Failed to create schema", e);
			return null;
		}
	}

	protected long validate(String assertionUuid, String prospective, String previous) throws Exception {
		if (assertionUuid == null || assertionUuid.isBlank() || prospective == null || prospective.isBlank() || previous == null || previous.isBlank()) {
			return -1L;
		}

		Assertion assertion = assertionService.getAssertionByUuid(UUID.fromString(assertionUuid));
		Assertions.assertNotNull(assertion);

		long runId = RUN_ID.incrementAndGet();
		usedRunIds.add(runId);
		MysqlExecutionConfig mysqlExecutionConfig = new MysqlExecutionConfig(runId);
		mysqlExecutionConfig.setProspectiveVersion(prospective);
		mysqlExecutionConfig.setPreviousVersion(previous);
		mysqlExecutionConfig.setFailureExportMax(100);

		List<TestRunItem> items = assertionExecutionService.executeAssertionsConcurrently(List.of(assertion), mysqlExecutionConfig);
		mysqlFailuresExtractor.extractTestResults(items, mysqlExecutionConfig, List.of(assertion));

		List<TestRunItem> assertionItems = items.stream()
				.filter(i -> assertion.getUuid().equals(i.getAssertionUuid()))
				.toList();
		Assertions.assertFalse(assertionItems.isEmpty(), "Assertion " + assertionUuid + " was not executed.");

		return assertionItems.stream()
				.mapToLong(TestRunItem::getFailureCount)
				.sum();
	}

	protected void insertConcept(String schema, String table, long id, String effectiveTime, int active, String moduleId, long definitionStatusId) {
		String sql = String.format("INSERT INTO `%s`.`%s` (id, effectivetime, active, moduleid, definitionstatusid) VALUES (?, ?, ?, ?, ?)", schema, table);
		try (Connection connection = rvfDynamicDataSource.getConnection(schema);
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, id);
			statement.setString(2, effectiveTime);
			statement.setInt(3, active);
			statement.setString(4, moduleId);
			statement.setLong(5, definitionStatusId);
			statement.execute();
		} catch (Exception e) {
			LOGGER.error("insertConcept failed", e);
		}
	}

	protected void insertDescription(String schema, String table, long id, String effectiveTime, int active, String moduleId, long conceptId, String languageCode, long typeId, String term, long caseSignificanceId) {
		String sql = String.format("INSERT INTO `%s`.`%s` (id, effectivetime, active, moduleid, conceptid, languagecode, typeid, term, casesignificanceid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", schema, table);
		try (Connection connection = rvfDynamicDataSource.getConnection(schema);
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, id);
			statement.setString(2, effectiveTime);
			statement.setInt(3, active);
			statement.setString(4, moduleId);
			statement.setLong(5, conceptId);
			statement.setString(6, languageCode);
			statement.setLong(7, typeId);
			statement.setString(8, term);
			statement.setLong(9, caseSignificanceId);
			statement.execute();
		} catch (Exception e) {
			LOGGER.error("insertDescription failed", e);
		}
	}

	protected void insertRelationship(String schema, String table, long id, String effectiveTime, int active, String moduleId, long sourceId, long destinationId, int relationshipGroup, long typeId, long characteristicTypeId, long modifierId) {
		String sql = String.format("INSERT INTO `%s`.`%s` (id, effectivetime, active, moduleid, sourceid, destinationid, relationshipgroup, typeid, characteristictypeid, modifierid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", schema, table);
		try (Connection connection = rvfDynamicDataSource.getConnection(schema);
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, id);
			statement.setString(2, effectiveTime);
			statement.setInt(3, active);
			statement.setString(4, moduleId);
			statement.setLong(5, sourceId);
			statement.setLong(6, destinationId);
			statement.setInt(7, relationshipGroup);
			statement.setLong(8, typeId);
			statement.setLong(9, characteristicTypeId);
			statement.setLong(10, modifierId);
			statement.execute();
		} catch (Exception e) {
			LOGGER.error("insertRelationship failed", e);
		}
	}

	protected void insertRelationshipConcreteValues(String schema, String table, long id, String effectiveTime, int active, String moduleId, long sourceId, String value, int relationshipGroup, long typeId, long characteristicTypeId, long modifierId) {
		String sql = String.format("INSERT INTO `%s`.`%s` (id, effectivetime, active, moduleid, sourceid, value, relationshipgroup, typeid, characteristictypeid, modifierid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", schema, table);
		try (Connection connection = rvfDynamicDataSource.getConnection(schema);
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, id);
			statement.setString(2, effectiveTime);
			statement.setInt(3, active);
			statement.setString(4, moduleId);
			statement.setLong(5, sourceId);
			statement.setString(6, value);
			statement.setInt(7, relationshipGroup);
			statement.setLong(8, typeId);
			statement.setLong(9, characteristicTypeId);
			statement.setLong(10, modifierId);
			statement.execute();
		} catch (Exception e) {
			LOGGER.error("insertRelationshipConcreteValues failed", e);
		}
	}

	protected void insertReferenceSetMember(String schema, String table, String id, String effectiveTime, int active, String moduleId, String refsetId, long referencedComponentId, Object... additionalFields) {
		StringBuilder columns = new StringBuilder("id, effectivetime, active, moduleid, refsetid, referencedcomponentid");
		StringBuilder placeholders = new StringBuilder("?, ?, ?, ?, ?, ?");
		for (int i = 0; i < additionalFields.length; i += 2) {
			columns.append(", ").append(additionalFields[i]);
			placeholders.append(", ?");
		}
		String sql = String.format("INSERT INTO `%s`.`%s` (%s) VALUES (%s)", schema, table, columns, placeholders);
		try (Connection connection = rvfDynamicDataSource.getConnection(schema);
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, id);
			statement.setString(2, effectiveTime);
			statement.setInt(3, active);
			statement.setString(4, moduleId);
			statement.setString(5, refsetId);
			statement.setLong(6, referencedComponentId);
			for (int i = 0; i < additionalFields.length; i += 2) {
				statement.setObject(7 + (i / 2), additionalFields[i + 1]);
			}
			statement.execute();
		} catch (Exception e) {
			LOGGER.error("02dd4978-458e-4dbe-b5b3-db382ce2fe36 Failed to insert reference set member", e);
		}
	}

	private void dropCodeSystemVersion(String codeSystemVersion) {
		try {
			releaseDataManager.dropSchema(codeSystemVersion);
		} catch (Exception e) {
			LOGGER.error("Failed to drop schema {}", codeSystemVersion, e);
		}
	}
}
