package org.ihtsdo.rvf.execution.service.impl;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.util.RvfDynamicDataSource;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An implementation of the {@link org.ihtsdo.rvf.execution.service.AssertionExecutionService}
 */
@Service
public class AssertionExecutionServiceImpl implements AssertionExecutionService, InitializingBean {

	@Autowired
	AssertionService assertionService;
	@Resource(name = "dataSource")
	BasicDataSource dataSource;
	@Autowired
	RvfDynamicDataSource rvfDynamicDataSource;
	@Autowired
	ReleaseDataManager releaseDataManager;
	private String qaResulTableName;
	private final ObjectMapper mapper = new ObjectMapper();
	private String deltaTableSuffix = "d";
	private String snapshotTableSuffix = "s";
	private String fullTableSuffix = "f";

	private final Logger logger = LoggerFactory.getLogger(AssertionExecutionServiceImpl.class);

	@Override
	public void afterPropertiesSet() throws Exception {
		final String createSQLString = "CREATE TABLE IF NOT EXISTS " + qaResulTableName + "(RUN_ID BIGINT, ASSERTION_ID BIGINT, " + 
				"DETAILS VARCHAR(500), INDEX (RUN_ID), INDEX (ASSERTION_ID))";
		try (Connection connection = dataSource.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				statement.execute(createSQLString);
			}
		}
		catch (final SQLException e) {
			logger.error("Error initialising Results table. Nested exception is : " + e.fillInStackTrace());
		}
	}

	@Override
	public TestRunItem executeAssertionTest(final AssertionTest assertionTest, final Long executionId,
											final String prospectiveReleaseVersion, final String previousReleaseVersion) {

		return executeTest(assertionTest.getAssertion(), assertionTest.getTest(), executionId, prospectiveReleaseVersion, previousReleaseVersion);
	}

	@Override
	public Collection<TestRunItem> executeAssertionTests(final Collection<AssertionTest> assertionTests, final Long executionId,
														 final String prospectiveReleaseVersion, final String previousReleaseVersion) {
		final Collection<TestRunItem> items = new ArrayList<>();
		for(final AssertionTest at: assertionTests){
			items.add(executeAssertionTest(at, executionId, prospectiveReleaseVersion, previousReleaseVersion));
		}

		return items;
	}

	@Override
	public Collection<TestRunItem> executeAssertion(final Assertion assertion, final Long executionId,
													final String prospectiveReleaseVersion, final String previousReleaseVersion) {

		final Collection<TestRunItem> runItems = new ArrayList<>();

		//get tests for given assertion
		for(final Test test: assertionService.getTests(assertion))
		{
			runItems.add(executeTest(assertion, test, executionId, prospectiveReleaseVersion, previousReleaseVersion));
		}

		return runItems;
	}

	@Override
	public Collection<TestRunItem> executeAssertions(final Collection<Assertion> assertions, final Long executionId,
													 final String prospectiveReleaseVersion, final String previousReleaseVersion) {
		final Collection<TestRunItem> items = new ArrayList<>();
		for(final Assertion assertion : assertions){
			items.addAll(executeAssertion(assertion, executionId, prospectiveReleaseVersion, previousReleaseVersion));
		}

		return items;
	}
	
	/** Executes an update using statement sql and logs the time taken **/
	private int executeUpdateStatement (final Connection connection, final String sql) throws SQLException{
		final long startTime = System.currentTimeMillis();
		logger.info("Executing {} statement:", sql.replaceAll("\n", " " ).replaceAll("\t", ""));
		try (Statement statement = connection.createStatement()) {
			//try block will close statement in all circumstances
			final int result = statement.executeUpdate(sql);
			final long timeTaken = System.currentTimeMillis() - startTime;
			logger.info("Completed in {}ms, result = {}", timeTaken, result);
			return result;
		}
	}

	@Override
	public TestRunItem executeTest(final Assertion assertion, final Test test, final Long executionId, final String prospectiveReleaseVersion, final String previousReleaseVersion) {

		logger.info("Starting execution id = " + executionId);
		final Calendar startTime = Calendar.getInstance();

		// set prospective version as default schema to use since SQL has calls that do not specify schema name
		final String prospectiveSchemaName = releaseDataManager.getSchemaForRelease(prospectiveReleaseVersion);
		logger.info("Setting default catalog as : " + releaseDataManager.getSchemaForRelease(prospectiveReleaseVersion));

		final TestRunItem runItem = new TestRunItem();
		runItem.setTestTime(startTime.getTime());
		runItem.setExecutionId(String.valueOf(executionId));
		runItem.setTestType(test.getType().name());
		runItem.setAssertionText(assertion.getName());
		runItem.setAssertionUuid(assertion.getUuid());

		// get command from test and validate the included command object
		final ExecutionCommand command = test.getCommand();
		if (command != null)
		{
			// execute sql and get result
			// create a single connection for entire test and close it after running test - avoid creating too many connections
			try (Connection connection = rvfDynamicDataSource.getConnection(prospectiveSchemaName)) {
				executeCommand(assertion, executionId, prospectiveReleaseVersion, previousReleaseVersion, command, connection);
				extractTestResult(assertion, executionId, runItem);
			}
			catch (final SQLException e) {
				logger.warn("Failed to excute command {},Nested exception is : " + e.fillInStackTrace(), command);
				runItem.setFailureMessage("Error executing SQL command object. Nested exception : " + e.fillInStackTrace());
			}
			catch (final ConfigurationException e) {
				logger.warn("Failed to configure command {}, Nested exception is : " + e.fillInStackTrace(), command);
				runItem.setFailureMessage("Error configuring SQL command object. Nested exception : " + e.fillInStackTrace());
				
			}
			
		}
		else{
			throw new IllegalArgumentException("Test passed does not have associated execution command. Test: \n" + test);
		}

		runItem.setRunTime(Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis());
		try {
			logger.info(runItem.toString());
			//need to log json different to a json file rather than in the log
			logger.info("runItem as json = " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(runItem));
		} catch (final IOException e) {
			logger.warn("Failed to write runItem in json to log.", e);
		}
		return runItem;
	}
/*
	@Override
	public Collection<TestRunItem> executeTests(Collection<Test> tests, Long executionId,
												String prospectiveReleaseVersion, String previousReleaseVersion) {
		Collection<TestRunItem> items = new ArrayList<>();
		for(Test test: tests){
			items.add(executeTest(test, executionId, prospectiveReleaseVersion, previousReleaseVersion));
		}

		return items;
	}*/

	private void executeCommand(final Assertion assertion, final Long executionId,
			final String prospectiveReleaseVersion,
			final String previousReleaseVersion,
			final ExecutionCommand command, final Connection connection)
			throws SQLException, ConfigurationException {
		String[] parts = {""};
		if (command.getStatements().size() == 0)
		{
			final String sql = command.getTemplate();
			parts = sql.split(";");
		}
		else {
			parts = command.getStatements().toArray(new String[command.getStatements().size()]);
		}
		// parse sql to get select statement
		String selectSQL = null;
		final List<String> sqlStatements = transformSql(parts,executionId,assertion,prospectiveReleaseVersion,previousReleaseVersion);
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
			}
			else if (sqlStatement.startsWith("select")){
				//TODO need to verify this is required.
				logger.info("Select query found:" + sqlStatement);
				selectSQL = sqlStatement;
				try (PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
					try (ResultSet execResult = preparedStatement.executeQuery()) {
						final String insertSQL = "insert into " + qaResulTableName + " (run_id, assertion_id, details) values (?, ?, ?)";
						try (Connection qaDbConnecion = dataSource.getConnection()) {
							try (PreparedStatement insertStatement = qaDbConnecion.prepareStatement(insertSQL)) {
								while(execResult.next())
								{
									insertStatement.setLong(1, executionId);
									insertStatement.setLong(2, assertion.getId());
									insertStatement.setString(3, execResult.getString(3));
									insertStatement.addBatch();
								}
								// execute insert statement
								insertStatement.executeBatch();
								logger.debug("batch insert completed for test:" + assertion.getName());
							}
						}
					}
				}
			}
			else {
				if (sqlStatement.startsWith("create table")){
					// only add engine if we do not create using a like statement
					if (!(sqlStatement.contains("like") || sqlStatement.contains("as"))) {
						sqlStatement = sqlStatement + " ENGINE = MyISAM";
					}
				}
				executeUpdateStatement(connection, sqlStatement);
			}
		}
	}

	private List<String> transformSql(final String[] parts, final Long executionId, final Assertion assertion, final String prospectiveRelease, final String previousRelease) throws ConfigurationException {
		final List<String> result = new ArrayList<>();
		final String defaultCatalog = dataSource.getDefaultCatalog();
		final String prospectiveSchema = releaseDataManager.getSchemaForRelease(prospectiveRelease);
		final String previousReleaseSchema = releaseDataManager.getSchemaForRelease(previousRelease);
		
		//We need both these schemas to exist
		if (prospectiveSchema == null) {
			throw new ConfigurationException ("Failed to determine a prospective schema for release " + prospectiveRelease);
		}
		
		if (previousReleaseSchema == null) {
			throw new ConfigurationException ("Failed to determine a schema for previous release " + previousRelease);
		}
		for( String part : parts) {
			logger.debug("Original sql statement: {}", part);
			// remove all SQL comments - //TODO might throw errors for -- style comments
			final Pattern commentPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
			part = commentPattern.matcher(part).replaceAll("");
			// replace all substitutions for exec
			part = part.replaceAll("<RUNID>", String.valueOf(executionId));
			part = part.replaceAll("<ASSERTIONUUID>", String.valueOf(assertion.getId()));
			// watch out for any 's that users might have introduced
			part = part.replaceAll("qa_result", defaultCatalog+ "." + qaResulTableName);
			part = part.replaceAll("<PROSPECTIVE>", prospectiveSchema);
			part = part.replaceAll("<TEMP>", prospectiveSchema);
			part = part.replaceAll("<PREVIOUS>", previousReleaseSchema);
			part = part.replaceAll("<DELTA>", deltaTableSuffix);
			part = part.replaceAll("<SNAPSHOT>", snapshotTableSuffix);
			part = part.replaceAll("<FULL>", fullTableSuffix);
			part = part.replaceAll("<PREVIOUS-RELEASE-DATE>", previousRelease);
			part = part.replaceAll("<CURRENT-RELEASE-DATE>", prospectiveRelease);
			part.trim();
			logger.debug("Transformed sql statement: {}", part);
			result.add(part);
		}
		return result;
	}

	private void extractTestResult(final Assertion assertion, final Long executionId, final TestRunItem runItem)
			throws SQLException {
		/*
		 create a prepared statement for retrieving matching results.
		*/
		final String resultSQL = "select assertion_id, details from "+ dataSource.getDefaultCatalog() + "." + qaResulTableName + " where assertion_id = ? and run_id = ?";
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement resultStatement = connection.prepareStatement(resultSQL)) {
				// select results that match execution
				resultStatement.setLong(1, assertion.getId());
				resultStatement.setLong(2, executionId);
				try (ResultSet resultSet = resultStatement.executeQuery()) {
					String sqlQueryString = resultStatement.toString();
					sqlQueryString = sqlQueryString.substring(sqlQueryString.indexOf(":"));
					logger.info("Getting test result using SQL : " + resultStatement);
					long counter = 0;
					while (resultSet.next())
					{
						// only get first 10 failed results
						if (counter < 10) {
							runItem.addFirstNInstance(resultSet.getString(2));
						}
						counter++;
					}
					logger.info("counter = " + counter);
					// if counter is > 0, then we know there are failures
					runItem.setFailureCount(counter);
				}
			}
		}
	}

	public void setQaResulTableName(final String qaResulTableName) {
		this.qaResulTableName = qaResulTableName;
	}

	public void setDeltaTableSuffix(final String deltaTableSuffix) {
		this.deltaTableSuffix = deltaTableSuffix;
	}

	public void setSnapshotTableSuffix(final String snapshotTableSuffix) {
		this.snapshotTableSuffix = snapshotTableSuffix;
	}

	public void setFullTableSuffix(final String fullTableSuffix) {
		this.fullTableSuffix = fullTableSuffix;
	}
}
