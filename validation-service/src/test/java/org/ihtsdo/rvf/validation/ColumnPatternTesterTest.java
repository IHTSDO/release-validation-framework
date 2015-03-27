package org.ihtsdo.rvf.validation;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.rvf.validation.impl.CsvResultFormatter;
import org.ihtsdo.rvf.validation.impl.StreamTestReport;
import org.ihtsdo.rvf.validation.log.impl.TestValidationLogImpl;
import org.ihtsdo.rvf.validation.resource.ResourceProvider;
import org.junit.Test;

public class ColumnPatternTesterTest {

	private ColumnPatternTester tester;
	private TestReportable testReport;

	@Test
	public void testFileNotFound() throws Exception {
		final ResourceProvider resourceManager = new TestFileResourceProvider(new File(""));
		testReport = new StreamTestReport(new CsvResultFormatter(), new TestWriterDelegate(new StringWriter()), false);
		tester = new ColumnPatternTester(new TestValidationLogImpl(ColumnPatternTester.class), resourceManager, testReport);

		tester.runTests();

		assertEquals(1, testReport.getNumErrors());
		assertEquals(0, testReport.getNumSuccesses());
	}

	@Test
	public void testInvalidId() throws Exception {
		final String filename = "/der2_sRefset_SimpleMapDelta_INT_20140131.txt";
		executeRun(filename, false);

		assertEquals("the 2 invalid ids", 2, testReport.getNumErrors());
		assertEquals(229, testReport.getNumSuccesses());
	}

	@Test
	public void testAssociationRefSet() throws Exception {
		final String filename = "/der2_cRefset_AssociationReferenceDelta_20140731.txt";
		executeRun(filename, false);

		assertEquals("Not an RF2 specification file", 1, testReport.getNumErrors());
		assertEquals(0, testReport.getNumSuccesses());
	}

	@Test
	public void testOnlyHeaders() throws Exception {
		final String filename = "/der2_Refset_SimpleDelta_INT_20140131.txt";
		executeRun(filename, false);

		assertEquals(0, testReport.getNumErrors());
		assertEquals("The column headers should have been fine, there are 6 of them", 6, testReport.getNumSuccesses());
	}

	@Test
	public void testEmptyEffectiveTime() throws Exception {
		final String filename = "/sct2_TextDefinition_Delta-en_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals("Invalid Effective Time there are 9 rows hence 9 errors", 9, testReport.getNumErrors());
	}

	@Test
	public void testComplexMapEmptyId() throws Exception {
		final String filename = "/der2_iissscRefset_ComplexMapDelta_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals("No id by 9 rows", 9, testReport.getNumErrors());
	}

	@Test
	public void testExtraColumn() throws Exception {
		final String filename = "/der2_ssRefset_ModuleDependencyDelta_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals("Only 4 as we abort the rest", 4, testReport.getNumErrors());
	}

	@Test
	public void testExtraColumnAtEnd() throws Exception {
		final String filename = "/der2_ssRefset_ModuleDependencyDelta_INT_20140831.txt";
		executeRun(filename, false);

		assertEquals("only the 4 errors i column 3 rows", 4, testReport.getNumErrors());
	}

	@Test
	public void testInvalidColumnName() throws Exception {
		final String filename = "/sct2_Relationship_Full_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals("Just the header", 1, testReport.getNumErrors());
	}

	@Test
	public void testInvalidModuleId() throws Exception {
		final String filename = "/sct2_StatedRelationship_Snapshot_INT_20140731.txt";
		executeRun(filename, false);
		assertEquals("9 rows contain this invalid module id, so one error reported with a count of 9 row 1 being the first occurence", 9, testReport.getNumErrors());
		assertEquals("how many errors were actually written out", 1, testReport.getNumberRecordedErrors());
	}

	@Test
	public void testMissingId() throws Exception {
		final String filename = "/der2_cRefset_AssociationReferenceDelta_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals("1 header + 9 rows contain missing id", 10, testReport.getNumErrors());
	}

	@Test
	public void testMissingModuleId() throws Exception {
		final String filename = "/der2_cRefset_LanguageDelta-en_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals("1 header + 9 rows contain missing id", 10, testReport.getNumErrors());
	}

	@Test
	public void testMixedErrors() throws Exception {
		final String filename = "/der2_cRefset_LanguageSnapshot-en_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals("1 header + 9 rows contain missing id", 32, testReport.getNumErrors());
	}

	@Test
	public void testSpacesAtEnd() throws Exception {
		final String filename = "/rel2_Refset_SimpleDelta_INT_20140428.txt";
		executeRun(filename, false);

		assertEquals("1 row contains a tab a the end + 1 row contains 2 spaces at end + the last column of the row with spaces will fail column pattern test ", 3, testReport.getNumErrors());
	}

	@Test
	public void testTabsInBetween() throws Exception {
		final String filename = "/rel2_Refset_SimpleDelta_INT_20140428.txt";
		executeRun(filename, false);

		assertEquals("1 row contains a tab a the end + 1 row contains 2 spaces at end + the last column of the row with spaces will fail column pattern test ", 3, testReport.getNumErrors());
	}

	@Test
	public void testBlankRow() throws Exception {
		final String filename = "/rel2_Refset_SimpleDelta_INT_20130422.txt";
		executeRun(filename, false);

		assertEquals("1 blank row ", 1, testReport.getNumErrors());
	}

	public void executeRun(final String filename, final boolean writeSucceses) throws URISyntaxException {
		final File f = new File(getClass().getResource(filename).toURI());

		final ResourceProvider resourceManager = new TestFileResourceProvider(f);
		//testReport = new TestReport(new CsvResultFormatter());

		testReport = new StreamTestReport(new CsvResultFormatter(), new TestWriterDelegate(new StringWriter()), writeSucceses);
		tester = new ColumnPatternTester(new TestValidationLogImpl(ColumnPatternTester.class), resourceManager, testReport);

		tester.runTests();
	}

	class TestFileResourceProvider implements ResourceProvider {

		private final List<String> fileNames = new ArrayList<>();
		private final File file;

		public TestFileResourceProvider(final File file) {
			this.file = file;
			fileNames.add(file.getName());
		}

		@Override
		public BufferedReader getReader(final String name, final Charset charset) throws IOException {
			return new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		}

		@Override
		public String getFilePath() {
			return file.getAbsolutePath();
		}

		@Override
		public List<String> getFileNames() {
			return fileNames;
		}

		@Override
		public boolean match(final String name) {
			return false;
		}
	}

}
