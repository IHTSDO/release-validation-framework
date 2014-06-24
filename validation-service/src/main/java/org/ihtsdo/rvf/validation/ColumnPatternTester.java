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
import java.util.regex.PatternSyntaxException;

public class ColumnPatternTester {


    private final ValidationLog validationLog;
	private final ColumnPatternConfiguration configuration;
    private final ResourceManager resourceManager;
    private final TestReport testReport;

    private Map<String, Pattern> regexCache;

	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");
	private static final Pattern BOOLEAN_PATTERN = Pattern.compile("[0-1]");
	private static final Pattern COMPONENT_ID_PATTERN = Pattern.compile("^\\d{6,18}$");

	private static final String UTF_8 = "UTF-8";

    private static final String FILE_NAME_TEST_TYPE = "FileNameTest";
    private static final String COLUMN_COUNT_TEST_TYPE = "ColumnCountTest";
    private static final String COLUMN_NAME_TEST_TYPE = "ColumnNameTest";
    private static final String COLUMN_VALUE_TEST_TYPE = "ColumnValuesTest";
    private static final String COLUMN_BOOLEAN_TEST_TYPE = "ColumnBooleanTest";

	public ColumnPatternTester(ValidationLog validationLog, ColumnPatternConfiguration configuration, ResourceManager resourceManager, TestReport testReport) {
		this.validationLog = validationLog;
		this.configuration = configuration;
		this.resourceManager = resourceManager;
        this.testReport = testReport;
		regexCache = testConfigurationByPrecompilingRegexPatterns(configuration);
	}

	public void runTests() {

		// Stats
		Date startTime = new Date();
		int filesTested = 0;
		long linesTested = 0;
		// TODO: Report errors in a machine readable way for error organisation/navigation.
        // TODO: separate class to manage the file name File Pattern Tester testing
		for (ColumnPatternConfiguration.File file : configuration.getFile()) {
			String fileName = file.getName();
			if (fileName != null) {

				if (resourceManager.isFile(fileName)) {

					try {
						List<Column> columns = file.getColumn();
						int configColumnCount = columns.size();
						Boolean allowAdditionalColumns = file.isAllowAdditionalColumns();
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
										testDataValue(lineNumber, value, column, linesTested + "", startTime, fileName);
									}
								}
							} else {
								validationLog.assertionError("Column count on line {} does not match expectation: expected {}, actual {}", lineNumber, dataColumnCount, configColumnCount);
                                testReport.addFailure(linesTested + "", startTime, fileName, resourceManager.getFilePath(), null, COLUMN_COUNT_TEST_TYPE, null, null);
							}
						}
					} catch (IOException e) {
						validationLog.executionError("Problem reading file {}", fileName, e);
					}
				} else {
					validationLog.executionError("Unable to locate file {}", fileName);
                    testReport.addFailure(linesTested + "", startTime, fileName, resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, null, null);
				}
			} else {
				validationLog.configurationError("Filename is null.");
                testReport.addFailure(linesTested + "", startTime, "filename is null", resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, null, null);
			}
		}

		// TODO: Use startTest/endTest validation log methods instead.
		validationLog.info("{} files and {} lines tested in {} milliseconds.", filesTested, linesTested, (new Date().getTime() - startTime.getTime()));
	}

	private void testDataValue(long lineNumber, String value, Column column, String linesTested, Date startTime, String fileName) {
        // todo consider simplifying
		// UUID
		if (column.getUuid() != null) {
			if (!UUID_PATTERN.matcher(value).matches()) {
				validationLog.assertionError("Value does not match UUID pattern on line {}, column name '{}': value '{}'", lineNumber, column.getName(), value);
                testReport.addFailure(linesTested , startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, UUID_PATTERN.pattern(), value);
			}
		}

		// Boolean
		if (column.getBoolean() != null) {
			if (!"1".equals(value) && !"0".equals(value)) {
				validationLog.assertionError("Value does not match Boolean pattern on line {}, column name '{}': value '{}'", lineNumber, column.getName(), value);
                testReport.addFailure(linesTested , startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_BOOLEAN_TEST_TYPE, BOOLEAN_PATTERN.pattern(), value);
			}
		}

		// Value
		String expectedValue = column.getValue();
		if (expectedValue != null) {
			if (!expectedValue.equals(value)) {
				validationLog.assertionError("Value does not match Expected value on line {}, column name '{}': expected '{}', actual '{}'", lineNumber, column.getName(), expectedValue, value);
                testReport.addFailure(linesTested , startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, null, value);
			}
		}

		// Date Stamp
		Column.DateStamp dateStamp = column.getDateStamp();
		if (dateStamp != null) {
			if (!DATE_PATTERN.matcher(value).matches()) {
				validationLog.assertionError("Value does not match Date Stamp pattern on line {}, column name '{}': value '{}'", lineNumber, column.getName(), value);
                testReport.addFailure(linesTested , startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, DATE_PATTERN.pattern(), value);
			} else {
				BigInteger maxDate = dateStamp.getMaxDate();
				if (maxDate != null) {
					Integer valueInt = Integer.valueOf(value);
					int maxDateInt = maxDate.intValue();
					if (valueInt > maxDateInt) {
						validationLog.assertionError("Value is a date after the maximum date on line {}, column name '{}': maximum date '', actual date '{}'", lineNumber, column.getName(), maxDateInt, value);
                        testReport.addFailure(linesTested , startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, null, value);
					}
				}
			}
		}

		// Component SCT ID
		if (column.getSctid() != null) {
			if (!COMPONENT_ID_PATTERN.matcher(value).matches()) {
				validationLog.assertionError("Value does not match SCTID pattern on line {}, column name '{}': value '{}'", lineNumber, column.getName(), value);
                testReport.addFailure(linesTested , startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, COMPONENT_ID_PATTERN.pattern(), value);
			}
		}

		// Regex
		String regex = column.getRegex();
		if (regex != null) {
			if (regexCache.get(regex).matcher(value).matches()) {
				validationLog.assertionError("Value does not match custom regex pattern on line {}, column name '{}': pattern '{}', value '{}'", lineNumber, column.getName(), regex, value);
                testReport.addFailure(linesTested , startTime, fileName, resourceManager.getFilePath(), column.getName(), COLUMN_VALUE_TEST_TYPE, regex, value);
			}
		}
	}

	private void testHeaderValue(String value, Column column, String linesTested, Date startTime, String fileName) {
		String expectedColumnName = column.getName();
		if (!expectedColumnName.equals(value)) {
			validationLog.assertionError("Column name does not match expected value: expected '{}', actual '{}'", expectedColumnName, value);
            testReport.addFailure(linesTested, startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_NAME_TEST_TYPE, null, null);
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

}
