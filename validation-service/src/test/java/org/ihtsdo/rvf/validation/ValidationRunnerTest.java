package org.ihtsdo.rvf.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/testValidationServiceContext.xml"})
public class ValidationRunnerTest {

    @Test
    public void testExecute() {

    }

}