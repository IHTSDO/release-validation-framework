package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.rvf.assertion._1_0.Column;
import org.ihtsdo.rvf.assertion._1_0.ColumnPatternConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ColumnPatternTester {

	private final ValidationLog validationLog;
    private final ResourceManager resourceManager;
    private final TestReport testReport;
    private final ConfigurationFactory configurationFactory;

    private Map<ColumnType, PatternTest> columnTests;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("[0-1]");
    private static final Pattern SCTID_PATTERN = Pattern.compile("^\\d{6,18}$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern NON_ZERO_INTEGER_PATTERN = Pattern.compile("^[1-9][0-9]*$");
	private static final Pattern BLANK = Pattern.compile("^$");

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
			boolean releaseInputFile = fileName.startsWith("rel2");

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
                                        testDataValue(lineNumber + "-" + columnIndex, lineNumber, value, column, linesTested + "", startTime, fileName, releaseInputFile);
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

    private void testDataValue(String id, long lineNumber, String value, Column column, String linesTested, Date startTime, String fileName, boolean isReleaseInputFile) {

		ColumnType columnType = getColumnType(column, isReleaseInputFile);
		PatternTest columnTest = columnTests.get(columnType);

        if (columnTest != null) {
            if (columnTest.validate(column, lineNumber, value)) {
                testReport.addSuccess(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
                        columnTest.getTestType(), columnTest.getPatternString());
            } else {
                validationLog.assertionError(columnTest.getMessage(), columnTest.getErrorArgs());
                testReport.addError(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
                        columnTest.getTestType(), columnTest.getPatternString(), value);
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

	private ColumnType getColumnType(Column column, boolean isReleaseInputFile) {
		if (column.getUuid() != null) {
			if (isReleaseInputFile) {
				return ColumnType.REL_UUID;
			} else {
				return ColumnType.UUID;
			}
		} else if (column.getDateStamp() != null) {
			if (isReleaseInputFile) {
				return ColumnType.REL_Time;
			} else {
				return ColumnType.Time;
			}
		} else if (column.getBoolean() != null) {
			return ColumnType.Boolean;
		} else if (column.getSctid() != null) {
			if (isReleaseInputFile) {
				return ColumnType.REL_SCTID;
			} else {
				return ColumnType.SCTID;
			}
		} else if (column.getRegex() != null) {
			return ColumnType.REGEX;
		} else if (column.getInteger() != null) {
			return ColumnType.Integer;
		} else if (column.getNonZeroInteger() != null) {
			return ColumnType.NonZeroInteger;
		}
		return null;
	}

	private void testHeaderValue(String value, Column column, String linesTested, Date startTime, String fileName) {
        String expectedColumnName = column.getName();
        if (!expectedColumnName.equals(value)) {
            validationLog.assertionError("Column name does not match expected value: expected '{}', actual '{}'", expectedColumnName, value);
            testReport.addError("1-0", startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_NAME_TEST_TYPE, "Column name does not match " + expectedColumnName, value);
            // TODO: Should we stop testing the file if we reach this point?
        }
    }

    private Map<ColumnType, PatternTest> assembleColumnTests() {
        columnTests = new HashMap<>();
		columnTests.put(ColumnType.UUID, new PatternTest("uuid", "Value does not match UUID pattern on line {}, column name '{}': value '{}'", UUID_PATTERN));
		columnTests.put(ColumnType.REL_UUID, new PatternTest("uuid", "Value does not match UUID or Blank patterns on line {}, column name '{}': value '{}'", UUID_PATTERN, BLANK));
		columnTests.put(ColumnType.Time, new DateTimeTest("dateStamp", "Value does not match Time pattern on line {}, column name '{}': value '{}'"));
		columnTests.put(ColumnType.REL_Time, new RelDateTimeTest("dateStamp", "Value does not match Time pattern on line {}, column name '{}': value '{}'"));
		columnTests.put(ColumnType.Boolean, new BooleanPatternTest("boolean", "Value does not match Boolean pattern on line {}, column name '{}': value '{}'", BOOLEAN_PATTERN));
		columnTests.put(ColumnType.SCTID, new PatternTest("sctid", "Value does not match SCTID pattern on line {}, column name '{}': value '{}'", SCTID_PATTERN));
		columnTests.put(ColumnType.REL_SCTID, new PatternTest("sctid", "Value does not match SCTID pattern on line {}, column name '{}': value '{}'", SCTID_PATTERN));
        columnTests.put(ColumnType.Integer, new PatternTest("integer", "Value does not match the required pattern of numbers only on line {}, column name '{}': value '{}'", INTEGER_PATTERN));
        columnTests.put(ColumnType.NonZeroInteger, new PatternTest("integer", "Value does not match a number other than 0 on line {}, column name '{}': value '{}'", NON_ZERO_INTEGER_PATTERN));
        return columnTests;
    }

    private class PatternTest {

		protected final Pattern[] patterns;
		protected final String methodName;
		protected String errorMessage;
		protected Object[] errorArgs;

		public PatternTest(String methodName, String errorMessage, Pattern... patterns) {
			this.methodName = methodName;
			this.patterns = patterns;
			this.errorMessage = errorMessage;
        }

        public boolean validate(Column column, long lineNumber, String value) {
            errorArgs = new String[]{lineNumber + "", column.getName(), value};
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

		public String getPatternString() {
			return patterns.toString();
		}
	}

    private class BooleanPatternTest extends PatternTest {

        public BooleanPatternTest(String methodName, String errorMessage, Pattern pattern) {
            super(methodName, errorMessage, pattern);
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
        public boolean validate(Column column, long lineNumber, String value) {
            // Date Stamp
            Column.DateStamp dateStamp = column.getDateStamp();
            if (dateStamp != null) {
                if (!DATE_PATTERN.matcher(value).matches()) {
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

	private class RelDateTimeTest extends DateTimeTest {

		private RelDateTimeTest(String methodName, String errorMessage) {
			super(methodName, errorMessage);
		}

		@Override
		public boolean validate(Column column, long lineNumber, String value) {
			if (value.isEmpty()) {
				return true;
			} else {
				return super.validate(column, lineNumber, value);
			}
		}

	}

}
