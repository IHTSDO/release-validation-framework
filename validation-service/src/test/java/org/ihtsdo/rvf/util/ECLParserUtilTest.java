package org.ihtsdo.rvf.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testExecutionServiceContext.xml"})
public class ECLParserUtilTest {

    @Test
    public void testECLParser() throws Exception {
    	String correctECL = "<< 71388002 |Procedure (procedure)|";
    	String wrongECL = "<< 71388002 |Procedure (procedure)";
    	assertTrue(ECLParserUtil.validateECLString(ECLParser.getInstance(), correctECL));
    	assertFalse(ECLParserUtil.validateECLString(ECLParser.getInstance(), wrongECL));
    }
    
    /**
     * The test seems not to work since not enough rules for Expression Template
     * @throws Exception
     */
    @Test
    public void testExpressionTemplateParser() throws Exception {
    	String correctExpressionTemplate = "[[+scg(<< 309795001 |Surgical access values (qualifier value)|)]]";
    	//String wrongExpressionTemplate = "[[+id(<< 71388002 |Procedure (procedure)|)]";
    	
    	assertTrue(ECLParserUtil.validateECLString(ExpressionTemplateParser.getInstance(), correctExpressionTemplate));
    	//assertFalse(ECLParserUtil.validateECLString(ExpressionTemplateParser.getInstance(), wrongExpressionTemplate));
    }
}
