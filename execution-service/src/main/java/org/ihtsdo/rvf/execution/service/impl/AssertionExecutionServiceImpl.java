package org.ihtsdo.rvf.execution.service.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.naming.ConfigurationException;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.FailureDetail;
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
	private String deltaTableSuffix = "d";
	private String snapshotTableSuffix = "s";
	private String fullTableSuffix = "f";
	
	private ExecutorService executorService = Executors.newCachedThreadPool();

	private final Logger logger = LoggerFactory.getLogger(AssertionExecutionServiceImpl.class);

	@Override
	public void afterPropertiesSet() throws Exception {
		
	}

	@Override
	public TestRunItem executeAssertionTest(final AssertionTest assertionTest, final ExecutionConfig config) {

		return executeTest(assertionTest.getAssertion(), assertionTest.getTest(), config);
	}

	@Override
	public Collection<TestRunItem> executeAssertionTests(final Collection<AssertionTest> assertionTests, final ExecutionConfig config) {
		final Collection<TestRunItem> items = new ArrayList<>();
		for(final AssertionTest at: assertionTests){
			items.add(executeAssertionTest(at, config));
		}

		return items;
	}

	@Override
	public Collection<TestRunItem> executeAssertion(final Assertion assertion, final ExecutionConfig config) {

		final Collection<TestRunItem> runItems = new ArrayList<>();

		//get tests for given assertion
		for(final Test test: assertionService.getTests(assertion))
		{
			runItems.add(executeTest(assertion, test, config));
		}

		return runItems;
	}

	@Override
	public Collection<TestRunItem> executeAssertions(final Collection<Assertion> assertions, final ExecutionConfig config) {
		final Collection<TestRunItem> items = new ArrayList<>();
		for(final Assertion assertion : assertions){
			items.addAll(executeAssertion(assertion, config));
		}

		return items;
	}
	
	
	
	
@Override
public List<TestRunItem> executeAssertionsConcurrently(List<Assertion> assertions, final ExecutionConfig executionConfig) {
		
		final List<Future<Collection<TestRunItem>>> concurrentTasks = new ArrayList<>();
		final List<TestRunItem> results = new ArrayList<>();
		int counter = 1;
		List<Assertion> batch = null;
		for (final Assertion assertion: assertions) {
			if (batch == null) {
				batch = new ArrayList<Assertion>();
			}
			batch.add(assertion);
			if (counter % 10 == 0 || counter == assertions.size()) {
				final List<Assertion> work = batch;
				logger.info(String.format("Started executing assertion [%1s] of [%2s]", counter, assertions.size()));
				final Future<Collection<TestRunItem>> future = executorService.submit(new Callable<Collection<TestRunItem>>() {
					@Override
					public Collection<TestRunItem> call() throws Exception {
						return executeAssertions(work, executionConfig);
					}
				});
				logger.info(String.format("Finished executing assertion [%1s] of [%2s]", counter, assertions.size()));
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
	public TestRunItem executeTest(final Assertion assertion, final Test test, final ExecutionConfig config) {

		long timeStart = System.currentTimeMillis();
		logger.debug("Start executing assertion:" + assertion.getUuid());
		// set prospective version as default schema to use since SQL has calls that do not specify schema name
		final String prospectiveSchemaName = releaseDataManager.getSchemaForRelease(config.getProspectiveVersion());

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
			try (Connection connection = rvfDynamicDataSource.getConnection(prospectiveSchemaName)) {
				executeCommand(assertion, config, command, connection);
				long timeEnd = System.currentTimeMillis();
				runItem.setRunTime((timeEnd - timeStart));
			} catch (final Exception e) {
				e.printStackTrace();
				logger.warn("Failed to excute command {},Nested exception is : " + e.fillInStackTrace(), command.toString());
				runItem.setFailureMessage("Error executing SQL command object Nested exception : " + e.fillInStackTrace());
				return runItem;
			} 
		} else {
			runItem.setFailureMessage("Test does not have any associated execution command:" + test);
			return runItem;
		}
		
		try {
			long extractTimeStart = System.currentTimeMillis();
			extractTestResult(assertion, runItem, config);
			long extractTimeEnd = System.currentTimeMillis();
			runItem.setExtractResultInMillis((extractTimeEnd - extractTimeStart));
		} catch (SQLException e) {
			logger.warn("Failed to extract test result : " + e.fillInStackTrace());
			runItem.setFailureMessage("Error extracting test result. Nested exception : " + e.fillInStackTrace() + runItem);
		}
		logger.info(runItem.toString());
		return runItem;
	}

	private void executeCommand(final Assertion assertion, final ExecutionConfig config,
			final ExecutionCommand command, final Connection connection)
			throws SQLException, ConfigurationException {
		String[] parts = {""};
		if (command.getStatements().size() == 0)
		{
			final String sql = command.getTemplate();
			if (sql != null) {
				parts = sql.split(";");
			}
		}else {
			parts = command.getStatements().toArray(new String[command.getStatements().size()]);
		}
		// parse sql to get select statement
		final List<String> sqlStatements = transformSql(parts,assertion, config);
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
				logger.info("Select query found:" + sqlStatement);
				final Long executionId = config.getExecutionId();
				try (PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement)) {
					try (ResultSet execResult = preparedStatement.executeQuery()) {
						final String insertSQL = "insert into " + qaResulTableName + " (run_id, assertion_id, details) values (?, ?, ?)";
						try (Connection qaDbConnecion = dataSource.getConnection()) {
							try (PreparedStatement insertStatement = qaDbConnecion.prepareStatement(insertSQL)) {
								while(execResult.next())
								{
									insertStatement.setLong(1, executionId);
									insertStatement.setLong(2, assertion.getAssertionId());
									insertStatement.setString(3, execResult.getString(3));
									insertStatement.addBatch();
								}
								// execute insert statement
								insertStatement.executeBatch();
								logger.debug("batch insert completed for assertion:" + assertion.getAssertionText());
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

	private List<String> transformSql(String[] parts, Assertion assertion, ExecutionConfig config) throws ConfigurationException {
		final List<String> result = new ArrayList<>();
		final String defaultCatalog = dataSource.getDefaultCatalog();
		
		final String prospectiveRelease = config.getProspectiveVersion();
		final String previousRelease = config.getPreviousVersion();
		final String prospectiveSchema = releaseDataManager.getSchemaForRelease(prospectiveRelease);
		final String previousReleaseSchema = releaseDataManager.getSchemaForRelease(previousRelease);
		
		//We need both these schemas to exist
		if (prospectiveSchema == null) {
			throw new ConfigurationException ("Failed to determine a prospective schema for release " + prospectiveRelease);
		}
		
		if (config.isReleaseValidation() && !config.isFirstTimeRelease() && previousReleaseSchema == null) {
			throw new ConfigurationException ("Failed to determine a schema for previous release " + previousRelease);
		}
		for( String part : parts) {
			logger.debug("Original sql statement: {}", part);
			// remove all SQL comments - //TODO might throw errors for -- style comments
			final Pattern commentPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
			part = commentPattern.matcher(part).replaceAll("");
			// replace all substitutions for exec
			part = part.replaceAll("<RUNID>", String.valueOf(config.getExecutionId()));
			part = part.replaceAll("<ASSERTIONUUID>", String.valueOf(assertion.getAssertionId()));
			// watch out for any 's that users might have introduced
			part = part.replaceAll("qa_result", defaultCatalog+ "." + qaResulTableName);
			part = part.replaceAll("<PROSPECTIVE>", prospectiveSchema);
			part = part.replaceAll("<TEMP>", prospectiveSchema);
			if (previousReleaseSchema != null) {
				part = part.replaceAll("<PREVIOUS>", previousReleaseSchema);
			}
			part = part.replaceAll("<DELTA>", deltaTableSuffix);
			part = part.replaceAll("<SNAPSHOT>", snapshotTableSuffix);
			part = part.replaceAll("<FULL>", fullTableSuffix);
			part.trim();
			logger.debug("Transformed sql statement: {}", part);
			result.add(part);
		}
		return result;
}

	private void extractTestResult(final Assertion assertion, final TestRunItem runItem, final ExecutionConfig config)
			throws SQLException {
		/*
		 create a prepared statement for retrieving matching results.
		*/
		String resultSQL = "select concept_id, details from "+ dataSource.getDefaultCatalog() + "." + qaResulTableName + " where assertion_id = ? and run_id = ?";
		//use limit to save memory and improve performance for worst case when containing thousands of errors
		if (config.getFailureExportMax() > 0) {
			resultSQL = resultSQL + " limit ?";
		}
		try (Connection connection = dataSource.getConnection()) {
			long counter = 0;
			try (PreparedStatement preparedStatement = connection.prepareStatement(resultSQL)) {
				// select results that match execution
				preparedStatement.setLong(1, assertion.getAssertionId());
				preparedStatement.setLong(2, config.getExecutionId());
				if( config.getFailureExportMax() > 0) {
					preparedStatement.setLong(3, config.getFailureExportMax());
				}
				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					
					while (resultSet.next())
					{
						// only get first N failed results
						if (config.getFailureExportMax() < 0 || counter < config.getFailureExportMax()) {
							FailureDetail detail = new FailureDetail(resultSet.getString(1), resultSet.getString(2));
							runItem.addFirstNInstance(detail);
						}
						counter++;
					}
				}
			}
			
			if ( counter < config.getFailureExportMax() ) {
				runItem.setFailureCount(counter);
			} else {
				String totalSQL = "select count(*) total from "+ dataSource.getDefaultCatalog() + "." + qaResulTableName + " where assertion_id = ? and run_id = ?";
				try (PreparedStatement preparedStatement = connection.prepareStatement(totalSQL)) {
					// select results that match execution
					preparedStatement.setLong(1, assertion.getAssertionId());
					preparedStatement.setLong(2, config.getExecutionId());
					try (ResultSet resultSet = preparedStatement.executeQuery()) {
						if (resultSet.next()) {
							runItem.setFailureCount(new Long(resultSet.getInt(1)));
						}
					}
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
