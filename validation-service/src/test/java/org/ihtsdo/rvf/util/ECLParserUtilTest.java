package org.ihtsdo.rvf.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

public class ECLParserUtilTest {

    @Test
    public void testECLParser() throws Exception {
    	String correctECL = "<< 71388002 |Procedure (procedure)|";
    	String wrongECL = "<< 71388002 |Procedure (procedure)";
    	assertTrue(ECLParserUtil.validateECLString(LongECLGrammar.getInstance(), correctECL));
    	assertFalse(ECLParserUtil.validateECLString(LongECLGrammar.getInstance(), wrongECL));
    }
    

    @Test
    public void testExpressionTemplateParser() throws Exception {
    	String correctExpressionTemplate = "[[+scg(<< 309795001 |Surgical access values (qualifier value)|)]]";
    	String wrongExpressionTemplate = "[[+id(<< 71388002 |Procedure (procedure)|)]";
    	
    	assertTrue(ECLParserUtil.validateECLString(ExpressionTemplateGrammar.getInstance(), correctExpressionTemplate));
    	assertFalse(ECLParserUtil.validateECLString(ExpressionTemplateGrammar.getInstance(), wrongExpressionTemplate));
    }
}
