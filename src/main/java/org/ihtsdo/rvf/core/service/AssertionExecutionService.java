package org.ihtsdo.rvf.core.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.core.data.model.*;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.importer.AssertionGroupImporter.ProductName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import javax.naming.ConfigurationException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;


@Service
public class AssertionExecutionService {

	private static final String FAILED_TO_FIND_RVF_DB_SCHEMA = "Failed to find rvf db schema for ";
	@Autowired
	private AssertionService assertionService;
	@Resource(name = "dataSource")
	private BasicDataSource dataSource;
	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;
	@Value("${rvf.qa.result.table.name}")
	private String qaResulTableName;
	@Value("${rvf.validation.international.modules}")
	private String internationalModules;
	private final String deltaTableSuffix = "d";
	private final String snapshotTableSuffix = "s";
	private final String fullTableSuffix = "f";

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	private final Logger logger = LoggerFactory.getLogger(AssertionExecutionService.class);

	public TestRunItem executeAssertionTest(final AssertionTest assertionTest, final MysqlExecutionConfig config) {

		return executeTest(assertionTest.getAssertion(), assertionTest.getTest(), config);
	}

	public Collection<TestRunItem> executeAssertionTests(final Collection<AssertionTest> assertionTests, final MysqlExecutionConfig config) {
		final Collection<TestRunItem> items = new ArrayList<>();
		for(final AssertionTest at: assertionTests){
			items.add(executeAssertionTest(at, config));
		}

		return items;
	}

	public Collection<TestRunItem> executeAssertion(final Assertion assertion, final MysqlExecutionConfig config) {

		final Collection<TestRunItem> runItems = new ArrayList<>();
		//get tests for given assertion
		for(final Test test: assertionService.getTests(assertion))
		{
			runItems.add(executeTest(assertion, test, config));
		}

		return runItems;
	}

	public Collection<TestRunItem> executeAssertions(final Collection<Assertion> assertions, final MysqlExecutionConfig config) {
		final Collection<TestRunItem> items = new ArrayList<>();
		for(final Assertion assertion : assertions){
			items.addAll(executeAssertion(assertion, config));
		}

		return items;
	}

public List<TestRunItem> executeAssertionsConcurrently(List<Assertion> assertions, final MysqlExecutionConfig executionConfig) {

		final List<Future<Collection<TestRunItem>>> concurrentTasks = new ArrayList<>();
		final List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		List<Assertion> batch = null;
		for (final Assertion assertion: assertions) {
			if (batch == null) {
				batch = new ArrayList<>();
			}
			batch.add(assertion);
			if (counter % 10 == 0 || counter == assertions.size()) {
				final List<Assertion> work = batch;
				logger.info("Started executing assertion [{}] of [{}]", counter, assertions.size());
				final Future<Collection<TestRunItem>> future = executorService.submit(() -> executeAssertions(work, executionConfig));
				logger.info("Finished executing assertion [{}] of [{}]", counter, assertions.size());
				//reporting every 10 assertions
				concurrentTasks.add(future);
				batch = null;
			}
			counter++;
		}

		// Wait for all concurrent tasks to finish
		for (final Future<Collection<TestRunItem>> concurrentTask : concurrentTasks) {
			try {
				results.addAll(concurrentTask.get());
			} catch (ExecutionException | InterruptedException e) {
				logger.error("Thread interrupted while waiting for future result.", e);
			}
		}
		return results;
	}
	/** Executes an update using statement sql and logs the time taken **/
	private void executeUpdateStatement (final Connection connection, final String sql) throws SQLException{
		final long startTime = System.currentTimeMillis();
		logger.info("Executing {} statement:", sql.replaceAll("\n", " " ).replaceAll("\t", ""));
		try (Statement statement = connection.createStatement()) {
			//try block will close statement in all circumstances
			final int result = statement.executeUpdate(sql);
			final long timeTaken = System.currentTimeMillis() - startTime;
			logger.info("Completed in {}ms, result = {}", timeTaken, result);
		}
	}

	public TestRunItem executeTest(final Assertion assertion, final Test test, final MysqlExecutionConfig config) {

		long timeStart = System.currentTimeMillis();
		logger.debug("Start executing assertion: {}", assertion.getUuid());
		final TestRunItem runItem = new TestRunItem();
		runItem.setTestCategory(assertion.getKeywords());
		runItem.setAssertionText(assertion.getAssertionText());
		runItem.setAssertionUuid(assertion.getUuid());
		runItem.setSeverity(assertion.getSeverity());

		// get command from test and validate the included command object
		final ExecutionCommand command = test.getCommand();
		if (command != null)
		{
			// execute sql and get result
			// create a single connection for entire test and close it after running test - avoid creating too many connections
			try (Connection connection = rvfDynamicDataSource.getConnection(config.getProspectiveVersion())) {
				executeCommand(assertion, config, command, connection);
				long timeEnd = System.currentTimeMillis();
				runItem.setRunTime((timeEnd - timeStart));
			} catch (final Exception e) {
				logger.warn("Failed to execute command {},Nested exception is {}", command, e.getMessage(), e);
				runItem.setFailureMessage("Error executing SQL command object Nested exception : " + e.fillInStackTrace());
				return runItem;
			}
		} else {
			runItem.setFailureMessage("Test does not have any associated execution command:" + test);
			return runItem;
		}
		logger.info(runItem.toString());
		return runItem;
	}

	private void executeCommand(final Assertion assertion, final MysqlExecutionConfig config,
			final ExecutionCommand command, final Connection connection)
			throws SQLException, ConfigurationException {
		String[] parts = splitCommand(command);
		// parse sql to get select statement
		final List<String> sqlStatements = transformSql(parts, assertion, config);
		for (String sqlStatement: sqlStatements)
		{
			// remove any leading and train white space
			sqlStatement = sqlStatement.trim();
			if (sqlStatement.startsWith("call")) {
				logger.info("Start calling stored proecure {}", sqlStatement);
				try ( CallableStatement cs = connection.prepareCall(sqlStatement)) {
					cs.execute();
				}
				logger.info("End of calling stored proecure {}", sqlStatement);
			} else if (sqlStatement.toLowerCase().startsWith("select")){
				//TODO need to verify this is required.
				logger.info("Select query found: {}", sqlStatement);
				try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
					try (ResultSet execResult = preparedStatement.executeQuery()) {
						try (Connection qaDbConnection = dataSource.getConnection()) {
							final String insertSQL = "insert into ? (run_id, assertion_id, details) values (?, ?, ?)";
							try(PreparedStatement insertStatement = qaDbConnection.prepareStatement(insertSQL)) {
								while (execResult.next()) {
									insertStatement.setString(1, qaResulTableName);
									insertStatement.setLong(2, config.getExecutionId());
									insertStatement.setLong(3, assertion.getAssertionId());
									insertStatement.setString(4, execResult.getString(3));
									insertStatement.addBatch();
								}
								// execute insert statement
								insertStatement.executeBatch();
								logger.debug("batch insert completed for assertion: {}", assertion.getAssertionText());
							}
						}
					}
				}
			}
			else {
				if (sqlStatement.startsWith("create table")
					// only add engine if we do not create using a like statement
					&& (!(sqlStatement.contains("like") || sqlStatement.contains("as")))){
					sqlStatement = sqlStatement + " ENGINE = MyISAM";
				}
				executeUpdateStatement(connection, sqlStatement);
			}
		}
	}

	private String[] splitCommand(ExecutionCommand command) {
		String[] parts = {""};
		if (command.getStatements().isEmpty())
		{
			final String sql = command.getTemplate();
			if (sql != null) {
				parts = sql.split(";");
			}
		} else {
			parts = command.getStatements().toArray(new String[0]);
		}
		return parts;
	}

	private List<String> transformSql(String[] parts, Assertion assertion, MysqlExecutionConfig config) throws ConfigurationException {
		List<String> result = new ArrayList<>();
		String defaultCatalog = dataSource.getDefaultCatalog();
		String prospectiveSchema = config.getProspectiveVersion();
		final String[] nameParts = config.getProspectiveVersion().split("_");
		String defaultModuleId = StringUtils.hasLength(config.getDefaultModuleId()) ? config.getDefaultModuleId() : (nameParts.length >= 2 ? ProductName.toModuleId(nameParts[1]) : "NOT_SUPPLIED");
		String includedModules = String.join(",", config.getIncludedModules());
		String version = (nameParts.length >= 3 ? nameParts[2] : "NOT_SUPPLIED");

		String previousReleaseSchema = config.getPreviousVersion();
		String dependencyReleaseSchema = config.getExtensionDependencyVersion();
		validateSchemas(config, prospectiveSchema, previousReleaseSchema);

		for( String part : parts) {
			if ((part.contains("<PREVIOUS>") && previousReleaseSchema == null)
				|| (part.contains("<DEPENDENCY>") && dependencyReleaseSchema == null)) {
				continue;
			}

			logger.debug("Original sql statement: {}", part);
			final Pattern commentPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
			part = commentPattern.matcher(part).replaceAll("");
			// replace all substitutions for exec
			part = part.replaceAll("<RUNID>", String.valueOf(config.getExecutionId()));
			part = part.replaceAll("<ASSERTIONUUID>", String.valueOf(assertion.getAssertionId()));
			part = part.replaceAll("<MODULEID>", defaultModuleId);
			part = part.replaceAll("<MODULEIDS>", includedModules);
			part = part.replaceAll("<VERSION>", version);
			// watch out for any 's that users might have introduced
			part = part.replaceAll("qa_result", defaultCatalog+ "." + qaResulTableName);
			part = part.replaceAll("<PROSPECTIVE>", prospectiveSchema);
			part = part.replaceAll("<TEMP>", prospectiveSchema);
			part = part.replaceAll("<INTERNATIONAL_MODULES>", internationalModules);
			if (previousReleaseSchema != null) {
				part = part.replaceAll("<PREVIOUS>", previousReleaseSchema);
			}
			if (dependencyReleaseSchema != null) {
				part = part.replaceAll("<DEPENDENCY>", dependencyReleaseSchema);
			}
			part = part.replaceAll("<DELTA>", deltaTableSuffix);
			part = part.replaceAll("<SNAPSHOT>", snapshotTableSuffix);
			part = part.replaceAll("<FULL>", fullTableSuffix);
			part = part.trim();
			logger.debug("Transformed sql statement: {}", part);
			result.add(part);
		}
		return result;
	}

	private static void validateSchemas(MysqlExecutionConfig config, String prospectiveSchema, String previousReleaseSchema) throws ConfigurationException {
		//We need both these schemas to exist
		if (prospectiveSchema == null) {
			throw new ConfigurationException (FAILED_TO_FIND_RVF_DB_SCHEMA + "prospective release");
		}

		if (config.isReleaseValidation() && !config.isFirstTimeRelease() && previousReleaseSchema == null) {
			throw new ConfigurationException (FAILED_TO_FIND_RVF_DB_SCHEMA + "previous release");
		}
	}
}
