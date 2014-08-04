package org.ihtsdo.rvf.validation;

import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.snomed.util.rf2.schema.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

//import org.springframework.util.StringUtils;

public class ColumnPatternTester {

    private final ValidationLog validationLog;
    private final ResourceManager resourceManager;
    private final TestReportable testReport;

    private Map<ColumnType, PatternTest> columnTests;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("[0-1]");
    private static final Pattern SCTID_PATTERN = Pattern.compile("^\\d{6,18}$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern NON_ZERO_INTEGER_PATTERN = Pattern.compile("^[1-9][0-9]*$");
    private static final Pattern BLANK = Pattern.compile("^$");
    private static final Pattern NOT_BLANK = Pattern.compile("^(?=\\s*\\S).*$");

    private static final String UTF_8 = "UTF-8";

    private static final String FILE_NAME_TEST_TYPE = "FileNameTest";
    private static final String COLUMN_COUNT_TEST_TYPE = "ColumnCountTest";
    private static final String ROW_SPACE_TEST_TYPE = "RowSpaceTest";
    private static final String COLUMN_HEADING_TEST = "ColumnHeadingTest";
    private static final String COLUMN_VALUE_TEST_TYPE = "ColumnValuesTest";
    private static final String COLUMN_DATE_TEST_TYPE = "ColumnDateTest";
    private static final String COLUMN_BOOLEAN_TEST_TYPE = "ColumnBooleanTest";

    public ColumnPatternTester(ValidationLog validationLog, ResourceManager resourceManager, TestReportable testReport) {
        this.validationLog = validationLog;
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
            if (fileName == null) {
                validationLog.executionError("Null file");
                continue;
            }
            if (!fileName.endsWith("txt")) {
                testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, "RF2 Compilant filename", fileName, "Incorrect file extension, should end with a .txt");
                continue;
            }

            SchemaFactory schemaFactory = new SchemaFactory();
            TableSchema tableSchema;
            try {
                tableSchema = schemaFactory.createSchemaBean(fileName);
            } catch (FileRecognitionException e) {
                // log the problem and continue to the next file
                testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, "RF2 Compilant filename", fileName, e.getMessage());
                continue;
            }
            if (tableSchema == null) {
                // log the problem and continue to the next file
                testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, "RF2 Compilant filename", fileName, "unexpected filename format.");
                continue;
            }

            boolean releaseInputFile = fileName.startsWith("rel2");
            List<Field> fields = tableSchema.getFields();

            if (fields != null) {
                try {
                    BufferedReader reader = resourceManager.getReader(fileName, Charset.forName(UTF_8));
                    filesTested++;
                    String line;
                    long lineNumber = 0;

                    String[] columnData;
                    int configColumnCount = fields.size();

                    while ((line = reader.readLine()) != null) {
                        int columnIndex = 0;
                        linesTested++;
                        lineNumber++;
                        columnData = line.split("\t");

                        int dataColumnCount = columnData.length;

                        if (!(validateRow(startTime, fileName, line, lineNumber, configColumnCount, dataColumnCount))) {
                            continue;
                        }

                        for (Field column : fields) {
                            String value = columnData[columnIndex];
                            if (lineNumber == 1) {
                                // Test header value
                                testHeaderValue(value, column, startTime, fileName, columnIndex + "");
                            } else {
                                testDataValue(lineNumber + "-" + columnIndex, lineNumber, value, column, startTime, fileName, releaseInputFile);
                            }
                            columnIndex++;
                        }
                    }
                } catch (IOException e) {
                    validationLog.executionError("Problem reading file {}", fileName, e);
                    testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, "", fileName, "Unable to read the file");
                }

            } else {
                validationLog.executionError("Invalid fileName {} does not match the expected pattern ", fileName);
                testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, "", fileName, "valid release 2 filename");
            }
        }

        validationLog.info("{} files and {} lines tested in {} milliseconds.", filesTested, linesTested, (new Date().getTime() - startTime.getTime()));
    }

    public boolean validateRow(Date startTime, String fileName, String line, long lineNumber, int configColumnCount, int dataColumnCount) {

        if (dataColumnCount != configColumnCount) {
            validationLog.assertionError("Column count on line {} does not match expectation: expected {}, actual {}", lineNumber, configColumnCount, dataColumnCount);
            testReport.addError(lineNumber + "-0", startTime, fileName, resourceManager.getFilePath(), null, COLUMN_COUNT_TEST_TYPE, "", "" + configColumnCount, "" + dataColumnCount);
            // cannot continue at this point as any validation will be off
            return false;
        }

        // will catch extra tabs, spaces at the end of a line
        if (line.endsWith("\t") || line.endsWith(" ")) {
            // extra spaces lets see if it is at the end, can still continue testing
            validationLog.assertionError("Extra space at the end of line {}, expected {}, actual {}", lineNumber, line.trim(), line);
            testReport.addError(lineNumber + "-" + dataColumnCount + 1, startTime, fileName, resourceManager.getFilePath(), null, ROW_SPACE_TEST_TYPE, "", line, line.trim());
            // continue testing
            return true;
        }

        return true;
    }

    private void testDataValue(String id, long lineNumber, String value, Field column, Date startTime, String fileName, boolean isReleaseInputFile) {

        ColumnType columnType = getColumnType(column.getType(), isReleaseInputFile);

        PatternTest columnTest = columnTests.get(columnType);

        if (columnTest != null) {
            if (canBeBlank(value, column) || columnTest.validate(column, lineNumber, value)) {
                testReport.addSuccess(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
                        columnTest.getTestType(), columnTest.getPatternString());
            } else {
                String testedValue = StringUtils.isNoneEmpty(value) ? value : "No Value";
                validationLog.assertionError(columnTest.getMessage(), columnTest.getErrorArgs());
                testReport.addError(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
                        columnTest.getTestType(), columnTest.getPatternString(), testedValue, columnTest.getExpectedValue());
            }
        }
    }

    private boolean canBeBlank(String value, Field column) {
        return !column.isMandatory() && isBlank(value);
    }

    private ColumnType getColumnType(DataType type, boolean isReleaseInputFile) {
        switch (type) {
            case SCTID:
                return isReleaseInputFile ? ColumnType.REL_SCTID : ColumnType.SCTID;
            case UUID:
                return isReleaseInputFile ? ColumnType.REL_UUID : ColumnType.UUID;
            case BOOLEAN:
                return ColumnType.BOOLEAN;
            case STRING:
                return ColumnType.STRING;
            case INTEGER:
                return ColumnType.INTEGER;
            case TIME:
                return isReleaseInputFile ? ColumnType.REL_TIME : ColumnType.TIME;
            case SCTID_OR_UUID:
                return isReleaseInputFile ? ColumnType.REL_SCTID_OR_UUID : ColumnType.SCTID_OR_UUID;
        }
        return null;
    }

    private void testHeaderValue(String value, Field column, Date startTime, String fileName, String colIndex) {
        String expectedColumnName = column.getName();
        if (expectedColumnName == null) {
            validationLog.info("Column name is null actual '{}'", value);
            column.setName(value);
        } else if (!expectedColumnName.equalsIgnoreCase(value)) {
            validationLog.assertionError("Column name does not match expected value: expected '{}', actual '{}'", expectedColumnName, value);
            testReport.addError("1-" + colIndex, startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_HEADING_TEST, "", value, expectedColumnName);
        } else {
            testReport.addSuccess("1-" + colIndex, startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_HEADING_TEST, "");
        }
    }

    private Map<ColumnType, PatternTest> assembleColumnTests() {
        columnTests = new HashMap<>();
        columnTests.put(ColumnType.UUID, new PatternTest("uuid", "Value does not match UUID pattern on line {}, column name '{}': value '{}'",
                UUID_PATTERN));
        columnTests.put(ColumnType.SCTID_OR_UUID, new PatternTest("uuid", "Value does not match UUID or SCTID pattern on line {}, column name '{}': value '{}'",
                UUID_PATTERN, SCTID_PATTERN, BLANK));
        columnTests.put(ColumnType.REL_SCTID_OR_UUID, new PatternTest("uuid", "Value does not match UUID or SCTID pattern on line {}, column name '{}': value '{}'",
                UUID_PATTERN, SCTID_PATTERN, BLANK));
        columnTests.put(ColumnType.REL_UUID, new PatternTest("uuid", "Value does not match UUID or Blank patterns on line {}, column name '{}': value '{}'",
                UUID_PATTERN, BLANK));
        columnTests.put(ColumnType.TIME, new DateTimeTest("dateStamp", "Value does not match Time pattern on line {}, column name '{}': value '{}'"));
        columnTests.put(ColumnType.REL_TIME, new RelDateTimeTest("dateStamp", "Value does not match Time pattern on line {}, column name '{}': value '{}'"));
        columnTests.put(ColumnType.BOOLEAN, new BooleanPatternTest("boolean", "Value does not match Boolean pattern on line {}, column name '{}': value '{}'",
                "1 or 0", BOOLEAN_PATTERN));
        columnTests.put(ColumnType.SCTID, new PatternTest("sctid", "Value does not match SCTID pattern on line {}, column name '{}': value '{}'",
                SCTID_PATTERN));
        columnTests.put(ColumnType.REL_SCTID, new PatternTest("sctid", "Value does not match SCTID pattern on line {}, column name '{}': value '{}'", SCTID_PATTERN, BLANK));
        columnTests.put(ColumnType.INTEGER, new PatternTest("integer", "Value does not match the required pattern of numbers only on line {}, column name '{}': value '{}'", INTEGER_PATTERN));
        columnTests.put(ColumnType.NON_ZERO_INTEGER, new PatternTest("integer", "Value does not match a number other than 0 on line {}, column name '{}': value '{}'", NON_ZERO_INTEGER_PATTERN));
        columnTests.put(ColumnType.STRING, new PatternTest("string", "Value does not match expected on line {}, expected '{}': actual '{}'", NOT_BLANK, BLANK));
        return columnTests;
    }

    private class PatternTest {

        protected final Pattern[] patterns;
        protected final String methodName;
        private String expectedValue;
        protected String errorMessage;
        protected Object[] errorArgs;

        public PatternTest(String methodName, String errorMessage, Pattern... patterns) {
            this.methodName = methodName;
            this.patterns = patterns;
            this.errorMessage = errorMessage;
            this.expectedValue = getPatternString();
        }

        public boolean validate(Field column, long lineNumber, String value) {
            errorArgs = new String[]{lineNumber + "", column.getName(), value};

            // ignore a null value if this is the case
            if ((column.getType() == DataType.SCTID_OR_UUID) && isBlank(value)) return true;

            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).matches()) {
                    return true;
                }
            }
            validationLog.assertionError(errorMessage, lineNumber, column.getName(), value);
            return false;
        }

        public String getTestType() {
            return COLUMN_VALUE_TEST_TYPE;
        }

        public String getMessage() {
            return errorMessage;
        }

        public Object[] getErrorArgs() {
            return errorArgs;
        }

        public String getExpectedValue() {
            return expectedValue;
        }

        public void setExpectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
        }

        public String getPatternString() {
            StringBuilder builder = new StringBuilder();
            builder.append(patterns[0].toString());
            if (patterns.length > 1) {
                for (int i = 1; i < patterns.length; i++) {
                    Pattern pattern = patterns[i];
                    builder.append(" or ");
                    builder.append(pattern.toString());
                }
            }
            return builder.toString();
        }
    }

    public boolean isBlank(String value) {
        return BLANK.matcher(value).matches();
    }

    private class BooleanPatternTest extends PatternTest {

        public BooleanPatternTest(String methodName, String errorMessage, String expectedValue, Pattern pattern) {
            super(methodName, errorMessage, pattern);
            setExpectedValue(expectedValue);
        }

        @Override
        public boolean validate(Field column, long lineNumber, String value) {
            errorArgs = new String[]{lineNumber + "", "1 or 2", value};

            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).matches()) {
                    return true;
                }
            }
            validationLog.assertionError(errorMessage, lineNumber, "1 or 2", value);
            return false;
        }

        @Override
        public String getTestType() {
            return COLUMN_BOOLEAN_TEST_TYPE;
        }
    }

    private class DateTimeTest extends PatternTest {

        public DateTimeTest(String methodName, String errorMessage) {
            super(methodName, errorMessage, DATE_PATTERN);
        }

        @Override
        public boolean validate(Field column, long lineNumber, String value) {
            // Date Stamp
            if (!DATE_PATTERN.matcher(value).matches()) {
                errorArgs = new String[]{lineNumber + "", column.getName(), value};
                return false;
            }
            return true;
        }

        @Override
        public String getTestType() {
            return COLUMN_DATE_TEST_TYPE;
        }
    }

    private class RelDateTimeTest extends DateTimeTest {

        private RelDateTimeTest(String methodName, String errorMessage) {
            super(methodName, errorMessage);
        }

        @Override
        public boolean validate(Field column, long lineNumber, String value) {
            return value.isEmpty() || super.validate(column, lineNumber, value);
        }

    }

}
