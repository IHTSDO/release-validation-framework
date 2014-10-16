package org.ihtsdo.rvf.execution.service.impl;

import com.google.common.base.Preconditions;
import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.rvf.entity.*;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.util.TestRunItem;
import org.ihtsdo.rvf.helper.Configuration;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * An implementation of the {@link org.ihtsdo.rvf.execution.service.AssertionExecutionService}
 */
@Service
public class AssertionExecutionServiceImpl implements AssertionExecutionService, InitializingBean {

    @Autowired
    AssertionService assertionService;
    @Autowired
    DataSource qaDataSource;
    @Autowired
    DataSource dataSource;
    String qaResulTableName;
    String assertionIdColumnName;
    String assertionNameColumnName;
    String assertionDetailsColumnName;
    ObjectMapper mapper = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(AssertionExecutionServiceImpl.class);

    public void afterPropertiesSet() throws Exception {
        String createSQLString = "CREATE TABLE IF NOT EXISTS " + qaResulTableName + "(ASSERTION_ID BIGINT, " +
                " ASSERTION_TEXT VARCHAR(255), DETAILS VARCHAR(255))";
        qaDataSource.getConnection().createStatement().execute(createSQLString);
    }

    @Override
    public TestRunItem executeAssertionTest(AssertionTest assertionTest, long executionId) {

        Preconditions.checkNotNull(assertionTest, "Assertion test can not be null");
        return executeTest(assertionTest.getTest(), null, executionId);
    }

    @Override
    public Collection<TestRunItem> executeAssertionTests(Collection<AssertionTest> assertions, long executionId) {
        return null;
    }

    @Override
    public Collection<TestRunItem> executeAssertion(Assertion assertion, ReleaseCenter releaseCenter, long executionId) {

        Collection<TestRunItem> runItems = new ArrayList<>();

        Preconditions.checkNotNull(assertion, "Assertion passed can not be null");
        Preconditions.checkNotNull(releaseCenter, "Release Center passed can not be null");
        //get tests for given assertion
        for(Test test: assertionService.getTests(assertion, releaseCenter))
        {
            runItems.add(executeTest(test, releaseCenter, executionId));
        }

        return runItems;
    }

    @Override
    public Collection<TestRunItem> executeAssertions(Collection<Assertion> assertions, ReleaseCenter releaseCenter, long executionId) {
        return null;

    }

    @Override
    public TestRunItem executeTest(Test test, ReleaseCenter releaseCenter, long executionId) {

        Calendar startTime = Calendar.getInstance();
        TestRunItem runItem = new TestRunItem();
        runItem.setTestTime(Calendar.getInstance().getTime());
        runItem.setExecutionId(String.valueOf(executionId));
        runItem.setConfiguration(test.getConfiguration());
        runItem.setTestType(test.getType().name());

        // get command from test and validate the included command object
        ExecutionCommand command = test.getCommand();
        if(command != null)
        {
            // get test configuration
            Configuration testConfiguration = test.getConfiguration();
            if(command.validate(test.getType(), testConfiguration))
            {
                String sql = command.getTemplate();
                // get the command code and execute it
                for(String key : testConfiguration.getKeys())
                {
                    logger.info("key : value " + key + " : " + testConfiguration.getValue(key));
                    sql = sql.replaceAll(key, testConfiguration.getValue(key));
                }

                logger.info("sql = " + sql);
                // execute sql and get result
                try
                {
                    String insertSQL = "insert into " + qaResulTableName + " (assertion_id, assertion_text, details) values (?, ?, ?)";
                    PreparedStatement insertStatement = qaDataSource.getConnection().prepareStatement(insertSQL);

                    // parse sql to get select statement
                    String selectSQL = null;
                    String[] parts = sql.split(";");
                    for(String part: parts)
                    {
                        if(part.startsWith("select")){
                            logger.info("Set select query :" + part);
                            selectSQL = part;
                        }
                        else if(part.startsWith("insert")){
                            logger.info("Executing insert statement");
                            qaDataSource.getConnection().prepareStatement(part).executeUpdate();
                        }
                        else {
                            logger.info("Executing create statement");
                            qaDataSource.getConnection().prepareStatement(part).execute();
                        }
                    }

                    // now execute the select statement
                    if(selectSQL == null){
                        throw new IllegalArgumentException("Select SQL is missing in command : " + command);
                    }

                    PreparedStatement preparedStatement = qaDataSource.getConnection().prepareStatement(selectSQL);
                    ResultSet execResult = preparedStatement.executeQuery();
                    while(execResult.next())
                    {
                        insertStatement.setLong(1, test.getId());
                        insertStatement.setString(2, test.getName());
                        insertStatement.setString(3, execResult.getString(1));
                        // execute insert statement
                        insertStatement.executeUpdate();
                    }
                    execResult.close();
                    preparedStatement.close();
                    insertStatement.close();

                    // select results that match execution
                    String resultSQL = "select assertion_id, assertion_text, details from " + qaResulTableName + " where assertion_id = ?";
                    PreparedStatement resultStatement = qaDataSource.getConnection().prepareStatement(resultSQL);
                    resultStatement.setLong(1, test.getId());
                    resultStatement.executeQuery();
                    ResultSet resultSet = resultStatement.executeQuery();
                    String detail = null;
                    int counter = 0;
                    while (resultSet.next())
                    {
                        detail = detail + resultSet.getString(3);
                        counter++;
                    }
                    resultSet.close();
                    resultStatement.close();

                    // if counter is > 0, then we know there are failures
                    if(counter > 0)
                    {
                        runItem.setFailureMessage("Failed Item count : " + counter);
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

    @Override
    public Collection<TestRunItem> executeTests(Collection<Test> tests, ReleaseCenter releaseCenter, long executionId) {
        return null;

    }

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
}
