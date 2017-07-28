package org.ihtsdo.rvf.validation.contanst;

import org.ihtsdo.rvf.validation.ColumnPatternTester;
import org.ihtsdo.rvf.validation.RF2FileStructureTester;
import org.ihtsdo.rvf.validation.StructuralTestRunItem;
import org.ihtsdo.rvf.validation.model.ColumnType;
import org.ihtsdo.snomed.util.rf2.schema.Field;

/**
 * Created by Tin Le
 * on 6/19/2017.
 */
public class ErrorMessage {
    public  static String getErrorDescription(StructuralTestRunItem item) {
        switch (item.getTestType()) {
            case ColumnPatternTester.COLUMN_DATE_TEST_TYPE:
                return "Column: " + item.getColumnName() + " - Line: " + item.getLineNr() + " Message: wrong format Date, Expected 8 numbers but got " + item.getActualValue();
            case RF2FileStructureTester.TEST_TYPE:
                return "The last line endings must be a Carriage Return + Line Feed" ;
            case ColumnPatternTester.COLUMN_VALUE_TEST_TYPE:
                if(item.getExpectedValue().equalsIgnoreCase(ColumnPatternTester.SCTID_PATTERN.pattern())){
                    return "Column: " + item.getColumnName() + " - Line: " + item.getLineNr() + " Message: wrong format value, Expected 6 - 18 numbers but got " + item.getActualValue();
                }else if(item.getExpectedValue().equalsIgnoreCase(ColumnPatternTester.UUID_PATTERN.pattern())){
                return "Column: " + item.getColumnName() + " - Line: " + item.getLineNr() + " Message: wrong format value, Expected patern'"+ item.getExpectedValue() + "' Ex: 89e0cc2b-ba8d-55cd-bb02-6ba52dbc201f" +" but got " + item.getActualValue();
            }
            case ColumnPatternTester.COLUMN_BOOLEAN_TEST_TYPE:
                return "Column: " + item.getColumnName() + " - Line: " + item.getLineNr() + " Message: wrong format Boolean value, Expected " + item.getExpectedValue() + " but got " + item.getActualValue();
            case ColumnPatternTester.FILE_NAME_TEST_TYPE:
                if(item.getExpectedValue().contains("unexpected filename format.")){
                    return "RF2 Compilant filename unexpected filename format. Ex: der2_Refset_SimpleDelta_INT_20140131 but got " + item.getActualValue();
                }
        }
        return item.getActualExpectedValue();
    }
}
