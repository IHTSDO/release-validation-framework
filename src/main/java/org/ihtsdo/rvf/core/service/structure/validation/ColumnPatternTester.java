package org.ihtsdo.rvf.core.service.structure.validation;

import org.ihtsdo.rvf.core.service.structure.listing.ColumnType;
import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.ihtsdo.snomed.util.rf2.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class ColumnPatternTester {

	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{8}$");
	private static final Pattern BOOLEAN_PATTERN = Pattern.compile("[0-1]");
	private static final Pattern SCTID_PATTERN = Pattern.compile("^\\d{6,18}$");
	private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
	private static final Pattern BLANK = Pattern.compile("^$");
	private static final Pattern NOT_BLANK = Pattern.compile("^(?=\\s*\\S).*$");
	private static final String FILE_NAME_TEST_TYPE = "FileNameTest";
	private static final String COLUMN_COUNT_TEST_TYPE = "ColumnCountTest";
	private static final String ROW_SPACE_TEST_TYPE = "RowSpaceTest";
	private static final String EMPTY_ROW_TEST = "BlankRowTest";
	private static final String COLUMN_HEADING_TEST = "ColumnHeadingTest";
	private static final String COLUMN_VALUE_TEST_TYPE = "ColumnValuesTest";
	private static final String COLUMN_DATE_TEST_TYPE = "ColumnDateTest";
	private static final String COLUMN_BOOLEAN_TEST_TYPE = "ColumnBooleanTest";
	private static final String SCTID_PARTITION_TEST_TYPE = "SctIdPartitionTest";
	public static final String COMPLIANT_FILENAME = "RF2 Compliant filename";

	private enum Rf2CoreFileKind {
		CONCEPT, DESCRIPTION, TEXT_DEFINITION, RELATIONSHIP
	}

	private final ValidationLog validationLog;
	private final ResourceProvider resourceManager;
	private final TestReportable testReport;
	private Map<ColumnType, PatternTest> columnTests;
	
	private final Logger logger = LoggerFactory.getLogger(ColumnPatternTester.class.getName());

	public ColumnPatternTester(final ValidationLog validationLog, final ResourceProvider resourceManager, final TestReportable testReport) {
		this.validationLog = validationLog;
		this.resourceManager = resourceManager;
		this.testReport = testReport;
		columnTests = assembleColumnTests();
	}

	public void runTests() {

		// Stats
		final Date startTime = new Date();
		int filesTested = 0;
		long linesTested = 0;

		// for each config file (should only the one)
		final List<String> fileNames = resourceManager.getFileNames();
		final SchemaFactory schemaFactory = new SchemaFactory();
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<Long>> tasks = new ArrayList<>();
		for (final String fileName : fileNames) {
			Future<Long> task = executor.submit(() -> runTestForFile(fileName, schemaFactory));
			tasks.add(task);
		}
		
		for (Future<Long> task : tasks) {
			try {
				long totalLines = task.get();
				if (totalLines > 0) {
					filesTested++;
					linesTested += totalLines;
				}
			} catch (InterruptedException | ExecutionException e) {
				logger.error("Error occurred when executing column validations", e);
			}
		}
		validationLog.info("{} files and {} lines tested in {} milliseconds.", filesTested, linesTested, (new Date().getTime() - startTime.getTime()));
	}

	private long runTestForFile(String fileName, SchemaFactory schemaFactory) {
		final Date startTime = new Date();
		long linesTested = 0;
		if (fileName == null) {
			validationLog.executionError("Null file");
			return linesTested;
		}
		if (!fileName.endsWith("txt")) {
			testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, COMPLIANT_FILENAME,
					"Incorrect file extension, should end with a .txt", fileName,null);
			return linesTested;
		}

		TableSchema tableSchema;
		try {
			tableSchema = schemaFactory.createSchemaBean(fileName);
		} catch (final FileRecognitionException e) {
			// log the problem and continue to the next file
			testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, COMPLIANT_FILENAME, e.getMessage(), fileName, null);
			return linesTested;
		}
		if (tableSchema == null) {
			// log the problem and continue to the next file
			testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, COMPLIANT_FILENAME, "unrecognized filename", fileName,null);
			return linesTested;
		}

		final Rf2CoreFileKind coreFileKind = detectCoreFileKind(fileName);
		List<Field> fields = tableSchema.getFields();
		if (fields != null) {
			try (BufferedReader reader = resourceManager.getReader(fileName, StandardCharsets.UTF_8)) {
				String line;
				long lineNumber = 0;

				String[] columnData;
				final int configColumnCount = fields.size();

				while ((line = reader.readLine()) != null) {
					int columnIndex = 0;
					linesTested++;
					lineNumber++;
					columnData = line.split("\t");

					final int dataColumnCount = columnData.length;

					if (!(validateRow(startTime, fileName, line, lineNumber, configColumnCount, dataColumnCount))) {
						continue;
					}
					//check whether header fields not containing null values due to specific additional fields
					if ( (lineNumber == 1) && havingAdditionalFields(tableSchema)) {
						schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, line);
						fields = tableSchema.getFields();
					}
					for (final Field column : fields) {
						final String value = columnData[columnIndex];
						if (lineNumber == 1) {
							// Test header value
							testHeaderValue(value, column, startTime, fileName, columnIndex);
						} else {
							testDataValue(lineNumber + "-" + columnIndex, lineNumber, value, column, startTime, fileName, coreFileKind);
						}
						columnIndex++;
					}
				}
			} catch (final IOException e) {
				validationLog.executionError("Problem reading file {}", fileName, e);
				testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), null, FILE_NAME_TEST_TYPE, "", "Unable to read the file", null,null);
			}

		} else {
			String errorMsg = String.format("Invalid fileName {} does not match the expected pattern", fileName);
			validationLog.executionError(errorMsg);
			testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), "", FILE_NAME_TEST_TYPE, "", errorMsg, null,null);
		}
		return linesTested;
		
	}

	private boolean havingAdditionalFields(final TableSchema tableSchema) {
		for (final Field field : tableSchema.getFields()) {
			if (field.getName() == null) {
				return true;
			}
		}
		return false;
	}

	public boolean validateRow(final Date startTime, final String fileName, final String line, final long lineNumber, final int configColumnCount, final int dataColumnCount) {
		if (line.isEmpty()) {
			validationLog.assertionError("Empty line at line {}", lineNumber);
			testReport.addError(lineNumber + "-0", startTime, fileName, resourceManager.getFilePath(), "Empty Row", EMPTY_ROW_TEST, "", line, "expected data",lineNumber);
			return false;
		}
		if (dataColumnCount != configColumnCount) {
			validationLog.assertionError("Column count on line {} does not match expectation: " +
					" {}, actual {}", lineNumber, configColumnCount, dataColumnCount);
			testReport.addError(lineNumber + "-0", startTime, fileName, resourceManager.getFilePath(), "Column Count Mismatch", COLUMN_COUNT_TEST_TYPE, "", String.valueOf(dataColumnCount), String.valueOf(configColumnCount),lineNumber);
			// cannot continue at this point as any validation will be off
			return false;
		}

		// will catch extra tabs, spaces at the end of a line
		if (line.endsWith("\t") || line.endsWith(" ")) {
			// extra spaces lets see if it is at the end, can still continue testing
			validationLog.assertionError("Extra space at the end of line {}, expected {}, actual {}", lineNumber, line.trim(), line);
			testReport.addError(lineNumber + "-" + dataColumnCount + 1, startTime, fileName, resourceManager.getFilePath(), "End of Row Space", ROW_SPACE_TEST_TYPE, "", line, line.trim(),lineNumber);
			// continue testing
			return true;
		}

		return true;
	}

	private void testDataValue(final String id, final long lineNumber, final String value, final Field column, final Date startTime, final String fileName, final Rf2CoreFileKind coreFileKind) {

		final ColumnType columnType = getColumnType(column);

		final PatternTest columnTest = columnTests.get(columnType);

		if (columnTest != null) {
			if (canBeBlank(value, column)) {
				testReport.addSuccess(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
						columnTest.getTestType(), columnTest.getPatternString());
			} else if (!columnTest.validate(column, lineNumber, value)) {
				final String testedValue = value.isEmpty() ? "No Value" : value;
				validationLog.assertionError(columnTest.getMessage(), columnTest.getErrorArgs());
				testReport.addError(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
						columnTest.getTestType(), columnTest.getPatternString(), testedValue, columnTest.getExpectedValue(), lineNumber);
			} else if (coreFileKind != null && isNumericSctId(value) && !partitionMatchesCoreRf2File(coreFileKind, column, value)) {
				final String partition = extractPartitionDigits(value);
				final String expected = expectedPartitionLabel(coreFileKind, column.getName());
				validationLog.assertionError("SCTID partition identifier does not match RF2 file type on line {}, column '{}': value '{}'", lineNumber, column.getName(), value);
				testReport.addError(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
						SCTID_PARTITION_TEST_TYPE, String.format("partition %s", expected), String.format("partition %s (%s)", partition, value),  expected, lineNumber);
			} else {
				testReport.addSuccess(id, startTime, fileName, resourceManager.getFilePath(), column.getName(),
						columnTest.getTestType(), columnTest.getPatternString());
			}
		}
	}

	private static Rf2CoreFileKind detectCoreFileKind(String fileName) {
		if (fileName == null) {
			return null;
		}
		String n = fileName;
		if (n.startsWith("x")) {
			n = n.substring(1);
		}
		if (!(n.startsWith("sct2_") || n.startsWith("rel2_"))) {
			return null;
		}
		if (n.contains("_Concept_")) {
			return Rf2CoreFileKind.CONCEPT;
		}
		if (n.contains("_Description_")) {
			return Rf2CoreFileKind.DESCRIPTION;
		}
		if (n.contains("_TextDefinition_")) {
			return Rf2CoreFileKind.TEXT_DEFINITION;
		}
		if (n.contains("_RelationshipConcreteValues_") || n.contains("_StatedRelationship_") || n.contains("_Relationship_")) {
			return Rf2CoreFileKind.RELATIONSHIP;
		}
		return null;
	}

	private static boolean isNumericSctId(String value) {
		return value != null && SCTID_PATTERN.matcher(value).matches();
	}

	/**
	 * Two-digit partition: the two digits immediately before the check digit (the final digit of the SCTID).
	 */
	public static String extractPartitionDigits(String sctId) {
		if (!isNumericSctId(sctId) || sctId.length() < 3) {
			return null;
		}
		return sctId.substring(sctId.length() - 3, sctId.length() - 1);
	}

	private boolean partitionMatchesCoreRf2File(Rf2CoreFileKind kind, Field column, String sctId) {
		final DataType dataType = column.getType();
		if (dataType != DataType.SCTID && dataType != DataType.SCTID_OR_UUID) {
			return true;
		}
		if (dataType == DataType.SCTID_OR_UUID && !isNumericSctId(sctId)) {
			return true;
		}
		final String partition = extractPartitionDigits(sctId);
		if (partition == null) {
			return true;
		}
		final Set<String> allowed = allowedPartitions(kind, column.getName());
		return allowed.contains(partition);
	}

	private static Set<String> allowedPartitions(Rf2CoreFileKind kind, String columnName) {
		final boolean isComponentId = "id".equalsIgnoreCase(columnName);
		return switch (kind) {
			case CONCEPT -> Set.of("00", "10");
			case DESCRIPTION, TEXT_DEFINITION -> isComponentId ? Set.of("01", "11") : Set.of("00", "10");
			case RELATIONSHIP -> isComponentId ? Set.of("02", "12") : Set.of("00", "10");
		};
	}

	private static String expectedPartitionLabel(Rf2CoreFileKind kind, String columnName) {
		final Set<String> allowed = allowedPartitions(kind, columnName);
		return "partition " + String.join(" or ", allowed);
	}

	private boolean canBeBlank(final String value, final Field column) {
		return !column.isMandatory() && isBlank(value);
	}

	private ColumnType getColumnType(final Field field) {
        return switch (field.getType()) {
            case SCTID -> ColumnType.SCTID;
            case SCTID_OR_UUID -> ColumnType.REL_SCTID;
            case UUID -> field.isMandatory() ? ColumnType.UUID : ColumnType.REL_UUID;
            case TIME -> field.isMandatory() ? ColumnType.TIME : ColumnType.REL_TIME;
            case BOOLEAN -> ColumnType.BOOLEAN;
            case INTEGER -> ColumnType.INTEGER;
            case STRING -> ColumnType.STRING;
        };
    }

	private void testHeaderValue(final String value, final Field column, final Date startTime, final String fileName, final int colIndex) {
		final String expectedColumnName = column.getName();
		if (expectedColumnName == null) {
			validationLog.info("Column name in the {} file is expected to be null actual '{}' at column {}", fileName, value, colIndex+1);
			column.setName(value);
		} else if (!expectedColumnName.equalsIgnoreCase(value)) {
			validationLog.assertionError("Column name does not match expected value: expected '{}', actual '{}'", expectedColumnName, value);
			testReport.addError("1-" + colIndex, startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_HEADING_TEST, "", value, expectedColumnName, 1L);
		} else {
			testReport.addSuccess("1-" + colIndex, startTime, fileName, resourceManager.getFilePath(), expectedColumnName, COLUMN_HEADING_TEST, "");
		}
	}

	private Map<ColumnType, PatternTest> assembleColumnTests() {
		columnTests = new HashMap<>();

		columnTests.put(ColumnType.SCTID, new PatternTest("sctid", "Value does not match SCTID pattern on line {}, column name '{}': value '{}'",
				SCTID_PATTERN));
		columnTests.put(ColumnType.REL_SCTID, new PatternTest("sctid", "Value does not match SCTID or UUID pattern on line {}, column name '{}': value '{}'",
				SCTID_PATTERN, UUID_PATTERN));

		columnTests.put(ColumnType.UUID, new PatternTest("uuid", "Value does not match UUID pattern on line {}, column name '{}': value '{}'",
				UUID_PATTERN));
		columnTests.put(ColumnType.REL_UUID, new PatternTest("uuid", "Value does not match UUID or Blank patterns on line {}, column name '{}': value '{}'",
				UUID_PATTERN, BLANK));

		columnTests.put(ColumnType.TIME, new DateTimeTest("dateStamp", "Value does not match Time pattern on line {}, column name '{}': value '{}'"));
		columnTests.put(ColumnType.REL_TIME, new RelDateTimeTest("dateStamp", "Value does not match Time or Blank pattern on line {}, column name '{}': value '{}'"));

		columnTests.put(ColumnType.BOOLEAN, new BooleanPatternTest("boolean", "Value does not match Boolean pattern on line {}, column name '{}': value '{}'",
				"1 or 0", BOOLEAN_PATTERN));

		columnTests.put(ColumnType.INTEGER, new PatternTest("integer", "Value does not match the required pattern of numbers only on line {}, column name '{}': value '{}'", INTEGER_PATTERN));

		// TODO: I think the only thing we can really test here is the length of the string. KK
		columnTests.put(ColumnType.STRING, new PatternTest("string", "Value does not match expected on line {}, expected '{}': actual '{}'", NOT_BLANK, BLANK));

		return columnTests;
	}

	public boolean isBlank(final String value) {
		return BLANK.matcher(value).matches();
	}

	private class PatternTest {

		protected final Pattern[] patterns;
		protected final String methodName;
		protected String errorMessage;
		protected Object[] errorArgs;
		private String expectedValue;

		public PatternTest(final String methodName, final String errorMessage, final Pattern... patterns) {
			this.methodName = methodName;
			this.patterns = patterns;
			this.errorMessage = errorMessage;
			this.expectedValue = getPatternString();
		}

		public boolean validate(final Field column, final long lineNumber, final String value) {
			errorArgs = new String[]{lineNumber + "", column.getName(), value};

			// ignore a null value if this is the case
			if ((column.getType() == DataType.SCTID_OR_UUID) && isBlank(value)) return true;

			for (final Pattern pattern : patterns) {
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

		public void setExpectedValue(final String expectedValue) {
			this.expectedValue = expectedValue;
		}

		public String getPatternString() {
			final StringBuilder builder = new StringBuilder();
			builder.append(patterns[0].toString());
			if (patterns.length > 1) {
				for (int i = 1; i < patterns.length; i++) {
					final Pattern pattern = patterns[i];
					builder.append(" or ");
					builder.append(pattern.toString());
				}
			}
			return builder.toString();
		}
	}

	private class BooleanPatternTest extends PatternTest {

		public BooleanPatternTest(final String methodName, final String errorMessage, final String expectedValue, final Pattern pattern) {
			super(methodName, errorMessage, pattern);
			setExpectedValue(expectedValue);
		}

		@Override
		public boolean validate(final Field column, final long lineNumber, final String value) {
			errorArgs = new String[]{lineNumber + "", "1 or 2", value};

			for (final Pattern pattern : patterns) {
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

		public DateTimeTest(final String methodName, final String errorMessage) {
			super(methodName, errorMessage, DATE_PATTERN);
		}

		@Override
		public boolean validate(final Field column, final long lineNumber, final String value) {
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

		private RelDateTimeTest(final String methodName, final String errorMessage) {
			super(methodName, errorMessage);
		}

		@Override
		public boolean validate(final Field column, final long lineNumber, final String value) {
			return value.isEmpty() || super.validate(column, lineNumber, value);
		}

	}

}
