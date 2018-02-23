package org.ihtsdo.rvf.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apg.Grammar;
import apg.Parser;

/**
 * Created by NamLe on 5/16/2017.
 */
public class ECLParserUtil {
    private static final Logger logger = LoggerFactory.getLogger(ECLParserUtil.class);
    public static boolean validateECLString(Grammar grammar, String txt){
        if(StringUtils.isNotBlank(txt)){
            Parser parser = new Parser(grammar);
            parser.setInputString(txt);
            try {
                Parser.Result result = parser.parse();
                return result.success();
            } catch (Exception e) {
            	logger.error("Error: " + e);
            }
        }
        return true;
    }    
}
