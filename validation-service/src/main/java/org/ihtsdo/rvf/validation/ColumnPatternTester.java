package org.ihtsdo.rvf.validation;

import org.apache.commons.beanutils.BeanUtils;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.rvf.assertion._1_0.Column;
import org.ihtsdo.rvf.assertion._1_0.ColumnPatternConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ColumnPatternTester {

    private final ValidationLog validationLog;
    private final ResourceManager resourceManager;
    private final TestReport testReport;
    private final ConfigurationFactory configurationFactory;

    private Map<String, PatternTest> columnTests;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("[0-1]");
    private static final Pattern SCTID_PATTERN = Pattern.compile("^\\d{6,18}$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern ORDER_PATTERN = Pattern.compile("^[1-9][0-9]*$");

    private static final String UTF_8 = "UTF-8";

    private static final String FILE_NAME_TEST_TYPE = "FileNameTest";
    private static final String COLUMN_COUNT_TEST_TYPE = "ColumnCountTest";
    private static final String COLUMN_NAME_TEST_TYPE = "ColumnNameTest";
    private static final String COLUMN_VALUE_TEST_TYPE = "ColumnValuesTest";
    private static final String COLUMN_DATE_TEST_TYPE = "ColumnDateTest";
    private static final String COLUMN_BOOLEAN_TEST_TYPE = "ColumnBooleanTest";
    private static final String COLUMN_INTEGER_TEST_TYPE = "ColumnIntegerTest";

    public ColumnPatternTester(ValidationLog validationLog, ConfigurationFactory configurationFactory, ResourceManager resourceManager, TestReport testReport) {
        this.validationLog = validationLog;
        this.configurationFactory = configurationFactory;
        this.resourceManager = resourceManager;
        this.testReport = testReport;

        columnTests = assembleColumnTests();
    }

    public void runTests() {

        // Stats
        Date startTime = new Date();
        int filesTested = 0;
        long linesTested = 0;

        // for each config file (should only the one)
        List<String> fileNames = resourceManager.getFileNames();
        for (String fileName : fileNames) {

            ColumnPatternConfiguration configuration = configurationFactory.getConfiguration(fileName);

            if (configuration != null) {
                for (ColumnPatternConfiguration.File file : configuration.getFile()) {

                    try {
                        List<Column> columns = file.getColumn();
                        int configColumnCount = columns.size();
                        Boolean allowAdditionalColumns = file.isAllowAdditionalColumns() != null ? file.isAllowAdditionalColumns() : Boolean.FALSE;
                        BufferedReader reader = resourceManager.getReader(fileName, Charset.forName(UTF_8));
                        filesTested++;
                        String line;
                        long lineNumber = 0;

                        // Variables outside loop to force memory reuse.
                        String[] columnData;
                        String value;

                        while ((line = reader.readLine()) != null) {
                            linesTested++;
                            lineNumber++;
                            columnData = line.split("\t");
                            int dataColumnCount = columnData.length;
                            if (dataColumnCount == configColumnCount || (dataColumnCount > configColumnCount && allowAdditionalColumns)) {
                                for (int columnIndex = 0; columnIndex < dataColumnCount; columnIndex++) {
                                    Column column = columns.get(columnIndex);
                                    value = columnData[columnIndex];
                                    if (lineNumber == 1) {
                                        // Test header value
                                        testHeaderValue(value, column, linesTested + "", startTime, fileName);
                                    } else {
                                        // Test data value
                                        testDataValue(lineNumber + "-" + columnIndex, lineNumber, value, column, linesTested + "", startTime, fileName);
                                    }
                                }
                            } else {
                                validationLog.assertionError("Column count on line {} does not match expectation: expected {}, actual {}", lineNumber, configColumnCount, dataColumnCount);
                                testReport.addError(lineNumber + "-1", startTime, fileName, resourceManager.getFilePath(), null, COLUMN_COUNT_TEST_TYPE, "Column count does not match expected " + configColumnCount, "" + dataColumnCount);
                            }
                        }
                    } catch (IOException e) {
                        validationLog.executionError("Problem reading file {}", fileName, e);
                        testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, "File with name " + fileName + " not supported", null);
                    }
                }
            } else {
                validationLog.executionError("Invalid fileName {} does not match the expected pattern ", fileName);
                testReport.addError(fileName, startTime, fileName, resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, null, null);
            }
        }
        // TODO: Use startTest/endTest validation log methods instead.
        validationLog.info("{} files and {} lines tested in {} milliseconds.", filesTested, linesTested, (new Date().getTime() - startTime.getTime()));
    }

    private void testDataValue(String id, long lineNumber, String value, Column column, String linesTested, Date startTime, String fileName) {

        ColumnTest columnTest = columnTests.get(column.getName());

        if (columnTest != null) {
            if (columnTest.validate(column, lineNumber, value)) {
                testReport.addSuccess(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
                        columnTest.getTestType(), columnTest.getPattern());
            } else {
                validationLog.assertionError(columnTest.getMessage(), columnTest.getErrorArgs());
                testReport.addError(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
                        columnTest.getTestType(), columnTest.getPattern(), value);
            }
        }

        // Regex
        String regex = column.getRegex();
        if (regex != null) {
            if (configurationFactory.getRegexCache().get(regex).matcher(value).matches()) {
                validationLog.assertionError("Value does not match custom regex pattern on line {}, column name '{}': pattern '{}', value '{}'", lineNumber, column.getName(), regex, value);
                testReport.addError(id, startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, regex, value);
            }
        }
    }

    private void testHeaderValue(String value, Column column, String linesTested, Date startTime, String fileName) {
        String expectedColumnName = column.getName();
        if (!expectedColumnName.equals(value)) {
            validationLog.assertionError("Column name does not match expected value: expected '{}', actual '{}'", expectedColumnName, value);
            testReport.addError("1-0", startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_NAME_TEST_TYPE, "Column name does not match " + expectedColumnName, value);
            // TODO: Should we stop testing the file if we reach this point?
        }
    }

    private Map<String, PatternTest> assembleColumnTests() {
        columnTests = new LinkedHashMap<>();
        columnTests.put("id", new PatternTest("uuid", UUID_PATTERN, "Value does not match UUID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("effectiveTime", new DateTimeTest("dateStamp", DATE_PATTERN, "Value does not match Date Stamp pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("sourceEffectiveTime", new DateTimeTest("dateStamp", DATE_PATTERN, "Value does not match Date Stamp pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("targetEffectiveTime", new DateTimeTest("dateStamp", DATE_PATTERN, "Value does not match Date Stamp pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("active", new BooleanPatternTest("boolean", BOOLEAN_PATTERN, "Value does not match Boolean pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("moduleId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("refSetId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("referencedComponentId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("mapGroup", new PatternTest("integer", INTEGER_PATTERN, "Value does not match the required pattern of numbers only on line {}, column name '{}': value '{}'"));
        columnTests.put("mapPriority", new PatternTest("integer", INTEGER_PATTERN, "Value does not match the required pattern of numbers only on line {}, column name '{}': value '{}'"));
        columnTests.put("descriptionLength", new PatternTest("integer", INTEGER_PATTERN, "descriptionLength does not match the required pattern of numbers only on line {}, column name '{}': value '{}'"));
        columnTests.put("correlationId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("mapCategoryId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("order", new PatternTest("integer", ORDER_PATTERN, "Value does not match a number other than 0 on line {}, column name '{}': value '{}'"));
        columnTests.put("linkedToId", new PatternTest("sctid", SCTID_PATTERN, "linkedToId does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("valueId", new PatternTest("sctid", SCTID_PATTERN, "valueId does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("acceptabilityId", new PatternTest("sctid", SCTID_PATTERN, "acceptabilityId does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("targetComponentId", new PatternTest("sctid", SCTID_PATTERN, "targetComponentId does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("descriptionFormat", new PatternTest("sctid", SCTID_PATTERN, "descriptionFormat does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        return columnTests;
    }

    private abstract class ColumnTest {

        public ColumnTest(String methodName, Pattern pattern, String errorMessage) {
            this.methodName = methodName;
            this.pattern = pattern;
            this.errorMessage = errorMessage;
        }

        abstract boolean validate(Column column, long lineNumber, String value);

        public String getTestType() {
            return COLUMN_VALUE_TEST_TYPE;
        }

        public String getMessage() {
            return errorMessage;
        }

        public Object[] getErrorArgs() {
            return errorArgs;
        }

        public String getPattern() {
            return pattern != null ? pattern.pattern() : null;
        }

        protected final String methodName;
        protected final Pattern pattern;
        protected String errorMessage;
        protected Object[] errorArgs;
    }

    private class PatternTest extends ColumnTest {

        public PatternTest(String methodName, Pattern pattern, String errorMessage) {
            super(methodName, pattern, errorMessage);
        }

        public boolean validate(Column column, long lineNumber, String value) {
            errorArgs = new String[]{lineNumber + "", column.getName(), value};
            try {
                Object val = BeanUtils.getProperty(column, methodName);
                boolean success = (val == null || pattern.matcher(value).matches());
                if (!success) validationLog.assertionError(errorMessage, lineNumber, column.getName(), value);
                return success;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    private class BooleanPatternTest extends PatternTest {

        public BooleanPatternTest(String methodName, Pattern pattern, String errorMessage) {
            super(methodName, pattern, errorMessage);
        }

        @Override
        public String getTestType() {
            return COLUMN_BOOLEAN_TEST_TYPE;
        }
    }

/*    private class ValueTest extends PatternTest {

        public ValueTest(String methodName, Pattern pattern, String errorMessage) {
            super(methodName, pattern, errorMessage);
        }

        @Override
        public boolean validate(Column column, long lineNumber, String value) {
            String expectedValue = column.getValue();
            errorArgs = new String[]{lineNumber + "", column.getName(), expectedValue, value};
            return expectedValue == null || expectedValue.equals(value);
        }
    }*/

    private class DateTimeTest extends PatternTest {

        public DateTimeTest(String methodName, Pattern pattern, String errorMessage) {
            super(methodName, pattern, errorMessage);
        }

        @Override
        public boolean validate(Column column, long lineNumber, String value) {
            // Date Stamp
            Column.DateStamp dateStamp = column.getDateStamp();
            if (dateStamp != null) {
                if (!pattern.matcher(value).matches()) {
                    errorArgs = new String[]{lineNumber + "", column.getName(), value};
                    return false;
                } else {
                    BigInteger maxDate = dateStamp.getMaxDate();
                    if (maxDate != null) {
                        Integer valueInt = Integer.valueOf(value);
                        int maxDateInt = maxDate.intValue();
                        if (valueInt > maxDateInt) {
                            errorArgs = new Object[]{lineNumber, column.getName(), maxDateInt, value};
                            errorMessage = "Value is a date after the maximum date on line {}, column name '{}': maximum date '', actual date '{}'";
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public String getTestType() {
            return COLUMN_DATE_TEST_TYPE;
        }
    }
}
