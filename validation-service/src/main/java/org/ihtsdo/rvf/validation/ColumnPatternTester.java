package org.ihtsdo.rvf.validation;

import org.apache.commons.beanutils.BeanUtils;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.rvf.assertion._1_0.Column;
import org.ihtsdo.rvf.assertion._1_0.ColumnPatternConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ColumnPatternTester {

    private final ValidationLog validationLog;
	private final ColumnPatternConfiguration configuration;
    private final ResourceManager resourceManager;
    private final TestReport testReport;

    private Map<String, Pattern> regexCache;
    private Map<String, PatternTest> columnTests;

	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");
	private static final Pattern BOOLEAN_PATTERN = Pattern.compile("[0-1]");
	private static final Pattern SCTID_PATTERN = Pattern.compile("^\\d{6,18}$");
	private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

	private static final String UTF_8 = "UTF-8";

    private static final String FILE_NAME_TEST_TYPE = "FileNameTest";
    private static final String COLUMN_COUNT_TEST_TYPE = "ColumnCountTest";
    private static final String COLUMN_NAME_TEST_TYPE = "ColumnNameTest";
    private static final String COLUMN_VALUE_TEST_TYPE = "ColumnValuesTest";
    private static final String COLUMN_DATE_TEST_TYPE = "ColumnDateTest";
    private static final String COLUMN_BOOLEAN_TEST_TYPE = "ColumnBooleanTest";
    //private static final String COLUMN_INTEGER_TEST_TYPE = "ColumnIntegerTest";

	public ColumnPatternTester(ValidationLog validationLog, ColumnPatternConfiguration configuration, ResourceManager resourceManager, TestReport testReport) {
		this.validationLog = validationLog;
		this.configuration = configuration;
		this.resourceManager = resourceManager;
        this.testReport = testReport;
		regexCache = testConfigurationByPrecompilingRegexPatterns(configuration);
        columnTests = assembleColumnTests();
	}

    public void runTests() {

		// Stats
		Date startTime = new Date();
		int filesTested = 0;
		long linesTested = 0;

		// TODO: Report errors in a machine readable way for error organisation/navigation.
		for (ColumnPatternConfiguration.File file : configuration.getFile()) {
			String fileName = file.getName();
			if (fileName != null) {

				if (resourceManager.isFile(fileName)) {

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
										testHeaderValue(value, column, linesTested+ "", startTime, fileName);
									} else {
										// Test data value
										testDataValue(columnData[0], lineNumber, value, column, linesTested + "", startTime, fileName);
									}
								}
							} else {
								validationLog.assertionError("Column count on line {} does not match expectation: expected {}, actual {}", lineNumber, dataColumnCount, configColumnCount);
                                testReport.addError(linesTested + "", startTime, fileName, resourceManager.getFilePath(), null, COLUMN_COUNT_TEST_TYPE, null, null);
							}
						}
					} catch (IOException e) {
						validationLog.executionError("Problem reading file {}", fileName, e);
					}
				} else {
					validationLog.executionError("Unable to locate file {}", fileName);
                    testReport.addError(linesTested + "", startTime, fileName, resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, null, null);
				}
			} else {
				validationLog.configurationError("Filename is null.");
                testReport.addError(linesTested + "", startTime, "filename is null", resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, null, null);
			}
		}

		// TODO: Use startTest/endTest validation log methods instead.
		validationLog.info("{} files and {} lines tested in {} milliseconds.", filesTested, linesTested, (new Date().getTime() - startTime.getTime()));
	}

	private void testDataValue(String id, long lineNumber, String value, Column column, String linesTested, Date startTime, String fileName) {

        ColumnTest columnTest = columnTests.get(column.getName());

        if(columnTest != null) {
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
			if (regexCache.get(regex).matcher(value).matches()) {
				validationLog.assertionError("Value does not match custom regex pattern on line {}, column name '{}': pattern '{}', value '{}'", lineNumber, column.getName(), regex, value);
                testReport.addError(linesTested, startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, regex, value);
			}
		}
    }

	private void testHeaderValue(String value, Column column, String linesTested, Date startTime, String fileName) {
		String expectedColumnName = column.getName();
		if (!expectedColumnName.equals(value)) {
			validationLog.assertionError("Column name does not match expected value: expected '{}', actual '{}'", expectedColumnName, value);
            testReport.addError(linesTested, startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_NAME_TEST_TYPE, null, null);
			// TODO: Should we stop testing the file if we reach this point?
		}
	}

	private Map<String, Pattern> testConfigurationByPrecompilingRegexPatterns(ColumnPatternConfiguration configuration) {
		Map<String, Pattern> regexCache = new HashMap<>();
		for (ColumnPatternConfiguration.File file : configuration.getFile()) {
			for (Column column : file.getColumn()) {
				String regex = column.getRegex();
				if (regex != null) {
					try {
						Pattern compiledRegex = Pattern.compile(regex);
						regexCache.put(regex, compiledRegex);
					} catch (PatternSyntaxException e) {
						validationLog.configurationError("Regex invalid for file {} column {}", file.getName(), column.getName(), e);
					}
				}
			}
		}
		return regexCache;
	}
    private Map<String, PatternTest> assembleColumnTests() {
        columnTests = new LinkedHashMap<>();
        columnTests.put("id", new PatternTest("uuid", UUID_PATTERN, "Value does not match UUID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("effectiveTime", new DateTimeTest("dateStamp", DATE_PATTERN, "Value does not match Date Stamp pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("active", new BooleanPatternTest("boolean", BOOLEAN_PATTERN, "Value does not match Boolean pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("moduleId", new ValueTest("value", null, "Value does not match Expected value on line {}, column name '{}': expected '{}', actual '{}'"));
        columnTests.put("refSetId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("referencedComponentId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("mapGroup", new PatternTest("integer", INTEGER_PATTERN, "Value does not match the required pattern of numbers only on line {}, column name '{}': value '{}'"));
        columnTests.put("mapPriority", new PatternTest("integer", INTEGER_PATTERN, "Value does not match the required pattern of numbers only on line {}, column name '{}': value '{}'"));
        columnTests.put("correlationId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
        columnTests.put("mapCategoryId", new PatternTest("sctid", SCTID_PATTERN, "Value does not match SCTID pattern on line {}, column name '{}': value '{}'"));
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

    private class PatternTest extends ColumnTest{

        public PatternTest(String methodName, Pattern pattern, String errorMessage) {
            super(methodName, pattern, errorMessage);
        }

        public boolean validate(Column column, long lineNumber, String value) {
            errorArgs = new String[] {lineNumber + "", column.getName(), value};
            try {
                Object val = BeanUtils.getProperty(column, methodName);
                boolean success = (val == null || pattern.matcher(value).matches());
                if(!success) validationLog.assertionError(errorMessage, lineNumber, column.getName(), value);
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

    private class ValueTest extends PatternTest {

        public ValueTest(String methodName, Pattern pattern, String errorMessage) {
            super(methodName, pattern, errorMessage);
        }

        @Override
        public boolean validate(Column column, long lineNumber, String value) {
            String expectedValue = column.getValue();
            errorArgs = new String[] {lineNumber + "", column.getName(), expectedValue, value};
            return expectedValue == null || expectedValue.equals(value);
        }
    }

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
                    errorArgs = new String[] {lineNumber + "", column.getName(), value};
                    return false;
                } else {
                    BigInteger maxDate = dateStamp.getMaxDate();
                    if (maxDate != null) {
                        Integer valueInt = Integer.valueOf(value);
                        int maxDateInt = maxDate.intValue();
                        if (valueInt > maxDateInt) {
                            errorArgs = new Object[] {lineNumber, column.getName(), maxDateInt, value};
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
