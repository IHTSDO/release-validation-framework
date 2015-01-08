package org.ihtsdo.rvf.execution.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.ExecutionCommand;
import org.ihtsdo.rvf.entity.Test;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.util.RvfDynamicDataSource;
import org.ihtsdo.rvf.helper.Configuration;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Pattern;

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
	String qaResulTableName;
	String assertionIdColumnName;
	String assertionNameColumnName;
	String assertionDetailsColumnName;
	ObjectMapper mapper = new ObjectMapper();
	String deltaTableSuffix = "d";
	String snapshotTableSuffix = "s";
	String fullTableSuffix = "f";

	private final Logger logger = LoggerFactory.getLogger(AssertionExecutionServiceImpl.class);
	private PreparedStatement insertStatement;

	@Override
	public void afterPropertiesSet() throws Exception {
		String createSQLString = "CREATE TABLE IF NOT EXISTS " + qaResulTableName + "(RUN_ID BIGINT, ASSERTION_ID BIGINT, " +
				" ASSERTION_TEXT VARCHAR(255), DETAILS VARCHAR(255), INDEX (RUN_ID), INDEX (ASSERTION_ID))";
		String insertSQL = "insert into " + qaResulTableName + " (run_id, assertion_id, assertion_text, details) values (?, ?, ?, ?)";
		try {
			dataSource.getConnection().createStatement().execute(createSQLString);
			insertStatement = dataSource.getConnection().prepareStatement(insertSQL);
		}
		catch (SQLException e) {
			logger.error("Error initialising Results table. Nested exception is : " + e.fillInStackTrace());
		}
	}

	@Override
	public TestRunItem executeAssertionTest(AssertionTest assertionTest, Long executionId,
											String prospectiveReleaseVersion, String previousReleaseVersion) {

		return executeTest(assertionTest.getAssertion(), assertionTest.getTest(), executionId, prospectiveReleaseVersion, previousReleaseVersion);
	}

	@Override
	public Collection<TestRunItem> executeAssertionTests(Collection<AssertionTest> assertionTests, Long executionId,
														 String prospectiveReleaseVersion, String previousReleaseVersion) {
		Collection<TestRunItem> items = new ArrayList<>();
		for(AssertionTest at: assertionTests){
			items.add(executeAssertionTest(at, executionId, prospectiveReleaseVersion, previousReleaseVersion));
		}

		return items;
	}

	@Override
	public Collection<TestRunItem> executeAssertion(Assertion assertion, Long executionId,
													String prospectiveReleaseVersion, String previousReleaseVersion) {

		Collection<TestRunItem> runItems = new ArrayList<>();

		//get tests for given assertion
		for(Test test: assertionService.getTests(assertion))
		{
			runItems.add(executeTest(assertion, test, executionId, prospectiveReleaseVersion, previousReleaseVersion));
		}

		return runItems;
	}

	@Override
	public Collection<TestRunItem> executeAssertions(Collection<Assertion> assertions, Long executionId,
													 String prospectiveReleaseVersion, String previousReleaseVersion) {
		Collection<TestRunItem> items = new ArrayList<>();
		for(Assertion assertion : assertions){
			items.addAll(executeAssertion(assertion, executionId, prospectiveReleaseVersion, previousReleaseVersion));
		}

		return items;
	}
	
	/** Executes an update using statement sql and logs the time taken **/
	private int executeUpdateStatement (Connection connection, String sql, String typeMsg) throws SQLException{
		long startTime = System.currentTimeMillis();
		logger.info("Executing {} statement: {}", typeMsg, sql);
		try (Statement statement = connection.createStatement()) {
			//try block will close statement in all circumstances
			int result = statement.executeUpdate(sql);
			long timeTaken = System.currentTimeMillis() - startTime;
			logger.info("Completed {} in {}ms, result = {}",typeMsg, timeTaken, result);
			return result;
		}
	}

	@Override
	public TestRunItem executeTest(Assertion assertion, Test test, Long executionId, String prospectiveReleaseVersion, String previousReleaseVersion) {

		logger.info("Started execution id = " + executionId);
		Calendar startTime = Calendar.getInstance();

		// set prospective version as default schema to use since SQL has calls that do not specify schema name
		String prospectiveSchemaName = releaseDataManager.getSchemaForRelease(prospectiveReleaseVersion);
		logger.info("Setting default catalog as : " + releaseDataManager.getSchemaForRelease(prospectiveReleaseVersion));

		TestRunItem runItem = new TestRunItem();
		runItem.setTestTime(startTime.getTime());
		runItem.setExecutionId(String.valueOf(executionId));
		runItem.setTestType(test.getType().name());
		runItem.setAssertionText(assertion.toString());

		// get command from test and validate the included command object
		ExecutionCommand command = test.getCommand();
		if(command != null)
		{
			// get test configuration
			Configuration testConfiguration = test.getCommand().getConfiguration();
			if(command.validate(test.getType(), testConfiguration))
			{
				// execute sql and get result
				try
				{
					// create a single connection for entire test and close it after running test - avoid creating too many connections
					Connection connection = rvfDynamicDataSource.getConnection(prospectiveSchemaName);
					/*
					 create a prepared statement for retrieving matching results.
					 Moving this outside to the after properties method throws resource closed exception because the
					 connection does not stay open for the entire duration - needs MySQL server tweaks
					*/
					String resultSQL = "select assertion_id, assertion_text, details from "+ dataSource.getDefaultCatalog() + "." + qaResulTableName + " where assertion_id = ? and run_id = ?";
					PreparedStatement resultStatement = dataSource.getConnection().prepareStatement(resultSQL);

					String[] parts = {""};
					if(command.getStatements().size() == 0)
					{
						String sql = command.getTemplate();
						parts = sql.split(";");
					}
					else{
						parts = command.getStatements().toArray(new String[command.getStatements().size()]);
					}
					// parse sql to get select statement
					String selectSQL = null;
					for(String part: parts)
					{
						// remove all SQL comments - //TODO might throw errors for -- style comments
						Pattern commentPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
						part = commentPattern.matcher(part).replaceAll("");

						// replace all substitutions for exec
						part = part.replaceAll("<RUNID>", String.valueOf(executionId));
						part = part.replaceAll("<ASSERTIONUUID>", String.valueOf(test.getId()));
						// watch out for any 's that users might have introduced
						part = part.replaceAll("<ASSERTIONTEXT>", test.getName().replaceAll("'", ""));
						part = part.replaceAll("qa_result_table", dataSource.getDefaultCatalog()+"."+qaResulTableName);
						part = part.replaceAll("<PROSPECTIVE>", releaseDataManager.getSchemaForRelease(prospectiveReleaseVersion));
						part = part.replaceAll("<TEMP>", releaseDataManager.getSchemaForRelease(prospectiveReleaseVersion));
						part = part.replaceAll("<PREVIOUS>", releaseDataManager.getSchemaForRelease(previousReleaseVersion));
						part = part.replaceAll("<DELTA>", deltaTableSuffix);
						part = part.replaceAll("<SNAPSHOT>", snapshotTableSuffix);
						part = part.replaceAll("<FULL>", fullTableSuffix);
						part = part.replaceAll("<PREVIOUS-RELEASE-DATE>", previousReleaseVersion);
						part = part.replaceAll("<CURRENT-RELEASE-DATE>", prospectiveReleaseVersion);

						for(String key : testConfiguration.getKeys())
						{
							logger.info("key : value " + key + " : " + testConfiguration.getValue(key));
							part = part.replaceAll(key, testConfiguration.getValue(key));
						}

						// remove any leading and train white space
						part = part.trim();
						if(part.startsWith("select")){
							logger.info("Set select query :" + part);
							selectSQL = part;

							PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);
							logger.info("Created statement");
							ResultSet execResult = preparedStatement.executeQuery();
							logger.info("execResult = " + execResult);
							while(execResult.next())
							{
								insertStatement.setLong(1, executionId);
								insertStatement.setLong(2, test.getId());
								insertStatement.setString(3, test.getName());
								insertStatement.setString(4, execResult.getString(1));
								// execute insert statement
								insertStatement.executeUpdate();
							}
							execResult.close();
							preparedStatement.close();
						}
						else if(part.startsWith("insert")){
							executeUpdateStatement(connection, part, "insert");
						}
						else {
							if(part.startsWith("create table")){
								// only add engine if we do not create using a like statement
								if (!(part.contains("like") || part.contains("as"))) {
									part = part + " ENGINE = MyISAM";
								}
							}
							int result = executeUpdateStatement(connection, part,"create table");
							if(result > 0){
								logger.error("Error executing sql : " + part);
							}
						}
					}

					// select results that match execution
					resultStatement.setLong(1, test.getId());
					resultStatement.setLong(2, executionId);
					ResultSet resultSet = resultStatement.executeQuery();
					String sqlQueryString = resultStatement.toString();
					sqlQueryString = sqlQueryString.substring(sqlQueryString.indexOf(":"));
					logger.info("Getting failure count using SQL : " + resultStatement);
					long counter = 0;
					while (resultSet.next())
					{
						// only get first 10 failed results
						if (counter<10) {
							runItem.addFirstNInstance(resultSet.getString(3));
						}
						counter++;
					}
					resultSet.close();
					resultStatement.close();
					logger.info("counter = " + counter);
					// close connection
					connection.close();

					// if counter is > 0, then we know there are failures
					runItem.setFailureCount(counter);
					if(counter > 0)
					{
						runItem.setFailureMessage("SQL lookup for failures : " + sqlQueryString);
						runItem.setFailure(true);
					}
					else{
						runItem.setFailure(false);
					}
				}
				catch (SQLException e) {
					logger.warn("Nested exception is : " + e.fillInStackTrace());
					runItem.setFailureMessage("Error executing SQL passed as command object. Nested exception : " + e.fillInStackTrace());
				}
			}
			else {
				logger.warn("Error validating command.: " + command);
				runItem.setFailureMessage("Error validating command.: " + command);
			}
		}
		else{
			throw new IllegalArgumentException("Test passed does not have associated execution command. Test: \n" + test);
		}

		runItem.setRunTime(Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis());
		try {
			logger.info("runItem as json = " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(runItem));
		}
		catch (IOException e) {
			e.printStackTrace();
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

	public void setQaResulTableName(String qaResulTableName) {
		this.qaResulTableName = qaResulTableName;
	}

	public void setAssertionIdColumnName(String assertionIdColumnName) {
		this.assertionIdColumnName = assertionIdColumnName;
	}

	public void setAssertionNameColumnName(String assertionNameColumnName) {
		this.assertionNameColumnName = assertionNameColumnName;
	}

	public void setAssertionDetailsColumnName(String assertionDetailsColumnName) {
		this.assertionDetailsColumnName = assertionDetailsColumnName;
	}

	public void setDeltaTableSuffix(String deltaTableSuffix) {
		this.deltaTableSuffix = deltaTableSuffix;
	}

	public void setSnapshotTableSuffix(String snapshotTableSuffix) {
		this.snapshotTableSuffix = snapshotTableSuffix;
	}

	public void setFullTableSuffix(String fullTableSuffix) {
		this.fullTableSuffix = fullTableSuffix;
	}
}
