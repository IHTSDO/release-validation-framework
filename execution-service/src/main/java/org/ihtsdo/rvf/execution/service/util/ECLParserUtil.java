package org.ihtsdo.rvf.execution.service.util;

import apg.Parser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by NamLe on 5/16/2017.
 */
public class ECLParserUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ECLParserUtil.class);
    public static boolean validateECLString(String columnText){
        if(StringUtils.isNotEmpty(columnText)){
            Parser parser = new Parser(ECLParser.getInstance());
            parser.setInputString(columnText);
            try {
                parser.parse();
                return parser.getResult().success();
            } catch (Exception e) {
                LOGGER.error("Error: " + e);
            }
        }
        return false;
    }

    public static boolean validateECLStrings(String firstColumnText, String... columnTexts){
        if(columnTexts.length == 0){
            return true;
        }
        boolean result;
        Parser parser = new Parser(ECLParser.getInstance());
        if(StringUtils.isNotEmpty(firstColumnText)){
            parser.setInputString(firstColumnText);
            try {
                parser.parse();
                result = parser.getResult().success();
                if(!result){
                    return false;
                }
            } catch (Exception e) {
                LOGGER.error("Error: " + e);
            }
        }
        for(String columnText : columnTexts){
            if(StringUtils.isNotEmpty(columnText)){
                parser.setInputString(columnText);
                try {
                    parser.parse();
                    result = parser.getResult().success();
                    if(!result){
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error: " + e);
                }
            }
        }
        return true;
    }
}
