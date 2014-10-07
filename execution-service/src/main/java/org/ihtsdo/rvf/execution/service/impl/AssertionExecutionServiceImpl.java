package org.ihtsdo.rvf.execution.service.impl;

import com.google.common.base.Preconditions;
import org.ihtsdo.rvf.entity.*;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.util.TestRunItem;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * An implementation of the {@link org.ihtsdo.rvf.execution.service.AssertionExecutionService}
 */
@Service
public class AssertionExecutionServiceImpl implements AssertionExecutionService {

    @Autowired
    AssertionService assertionService;
    @Autowired
    DataSource qaDataSource;

    private final Logger logger = LoggerFactory.getLogger(AssertionExecutionServiceImpl.class);

    @Override
    public TestRunItem executeAssertionTest(AssertionTest assertionTest) {

        Calendar startTime = Calendar.getInstance();
        TestRunItem runItem = new TestRunItem();
        runItem.setTestTime(Calendar.getInstance().getTime());

        Preconditions.checkNotNull(assertionTest, "Assertion test can not be null");
        Test test = assertionTest.getTest();
        Preconditions.checkNotNull(test, "No test associated with assertion test");

        // get command from test and validate the included command object
        ExecutionCommand command = test.getCommand();
        runItem.setTestType(test.getType().name());
        if(command != null)
        {
            try
            {
                command.validate(test.getType());
                // get the command code and execute it

            }
            catch (Exception e) {
                logger.warn("Error validating command. Nested exception is : " + e.fillInStackTrace());
                runItem.setActualValue("Error validating command. Nested exception is : " + e.fillInStackTrace());
            }
        }
        else{
            logger.warn("Test passed does not have associated execution command. Test: \n" + test);
            runItem.setActualValue("Test passed does not have associated execution command. Test: \n" + test);
        }

        runItem.setRunTime(Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis());
        return runItem;
    }

    @Override
    public Collection<TestRunItem> executeAssertionTests(Collection<AssertionTest> assertions) {
        return null;
    }

    @Override
    public Collection<TestRunItem> executeAssertion(Assertion assertion, ReleaseCenter releaseCenter) {

        Collection<TestRunItem> runItems = new ArrayList<>();

        Preconditions.checkNotNull(assertion, "Assertion passed can not be null");
        Preconditions.checkNotNull(releaseCenter, "Release Center passed can not be null");
        //get tests for given assertion
        for(Test test: assertionService.getTests(assertion, releaseCenter))
        {
            // get command from test and validate the included command object
            ExecutionCommand command = test.getCommand();
            if(command != null)
            {
                try
                {
                    command.validate(test.getType());
                    // get the command code and execute it

                }
                catch (Exception e) {
                    logger.warn("Error validating command. Nested exception is : " + e.fillInStackTrace());
                }
            }
            else{
                throw new IllegalArgumentException("Test passed does not have associated execution command. Test: \n" + test);
            }
        }

        return runItems;
    }

    @Override
    public Collection<TestRunItem> executeAssertions(Collection<Assertion> assertions, ReleaseCenter releaseCenter) {
        return null;

    }

    @Override
    public TestRunItem executeTest(Test test, ReleaseCenter releaseCenter) {
        return null;

    }

    @Override
    public Collection<TestRunItem> executeTests(Collection<Test> tests, ReleaseCenter releaseCenter) {
        return null;

    }
}
