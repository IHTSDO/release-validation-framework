package org.ihtsdo.rvf.core.validation;

import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.ihtsdo.rvf.core.service.structure.validation.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ColumnPatternTesterTest {

	private ColumnPatternTester tester;
	private TestReportable testReport;

	@Test
	public void testFileNotFound() {
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

		assertEquals(2, testReport.getNumErrors(), "the 2 invalid ids");
		assertEquals(229, testReport.getNumSuccesses());
	}

	@Test
	public void testAssociationRefSet() throws Exception {
		final String filename = "/der2_cRefset_AssociationReferenceDelta_20140731.txt";
		executeRun(filename, false);

		assertEquals(1, testReport.getNumErrors(), "Not an RF2 specification file");
		assertEquals(0, testReport.getNumSuccesses());
	}

	@Test
	public void testOnlyHeaders() throws Exception {
		final String filename = "/der2_Refset_SimpleDelta_INT_20140131.txt";
		executeRun(filename, false);

		assertEquals(0, testReport.getNumErrors());
		assertEquals(6, testReport.getNumSuccesses(), "The column headers should have been fine, there are 6 of them");
	}

	@Test
	public void testEmptyEffectiveTime() throws Exception {
		final String filename = "/sct2_TextDefinition_Delta-en_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals(9, testReport.getNumErrors(), "Invalid Effective Time there are 9 rows hence 9 errors");
	}

	@Test
	public void testComplexMapEmptyId() throws Exception {
		final String filename = "/der2_iissscRefset_ComplexMapDelta_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals(9, testReport.getNumErrors(), "No id by 9 rows");
	}

	@Test
	public void testExtraColumn() throws Exception {
		final String filename = "/der2_ssRefset_ModuleDependencyDelta_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals(4, testReport.getNumErrors(), "Only 4 as we abort the rest");
	}

	@Test
	public void testExtraColumnAtEnd() throws Exception {
		final String filename = "/der2_ssRefset_ModuleDependencyDelta_INT_20140831.txt";
		executeRun(filename, false);

		assertEquals(4, testReport.getNumErrors(), "only the 4 errors i column 3 rows");
	}

	@Test
	public void testInvalidColumnName() throws Exception {
		final String filename = "/sct2_Relationship_Full_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals(1, testReport.getNumErrors(), "Just the header");
	}

	@Test
	public void testInvalidModuleId() throws Exception {
		final String filename = "/sct2_StatedRelationship_Snapshot_INT_20140731.txt";
		executeRun(filename, false);
		assertEquals(9, testReport.getNumErrors(), "9 rows contain this invalid module id, so one error reported with a count of 9 row 1 being the first occurence");
		assertEquals(1, testReport.getNumberRecordedErrors(), "how many errors were actually written out");
	}

	@Test
	public void testMissingId() throws Exception {
		final String filename = "/der2_cRefset_AssociationReferenceDelta_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals(10, testReport.getNumErrors(), "1 header + 9 rows contain missing id");
	}

	@Test
	public void testMissingModuleId() throws Exception {
		final String filename = "/der2_cRefset_LanguageDelta-en_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals(10, testReport.getNumErrors(), "1 header + 9 rows contain missing id");
	}

	@Test
	public void testMixedErrors() throws Exception {
		final String filename = "/der2_cRefset_LanguageSnapshot-en_INT_20140731.txt";
		executeRun(filename, false);

		assertEquals(32, testReport.getNumErrors(), "1 header + 9 rows contain missing id");
	}

	@Test
	public void testSpacesAtEnd() throws Exception {
		final String filename = "/rel2_Refset_SimpleDelta_INT_20140428.txt";
		executeRun(filename, false);

		assertEquals(3, testReport.getNumErrors(), "1 row contains a tab a the end + 1 row contains 2 spaces at end + the last column of the row with spaces will fail column pattern test ");
	}

	@Test
	public void testTabsInBetween() throws Exception {
		final String filename = "/rel2_Refset_SimpleDelta_INT_20140428.txt";
		executeRun(filename, false);

		assertEquals(3, testReport.getNumErrors(), "1 row contains a tab a the end + 1 row contains 2 spaces at end + the last column of the row with spaces will fail column pattern test ");
	}

	@Test
	public void testBlankRow() throws Exception {
		final String filename = "/rel2_Refset_SimpleDelta_INT_20130422.txt";
		executeRun(filename, false);

		assertEquals(1, testReport.getNumErrors(), "1 blank row ");
	}


	@Test
	public void testRelationshipConcreteValuesFile() throws Exception {
		final String filename = "/sct2_RelationshipConcreteValues_Delta_INT_20210131.txt";
		executeRun(filename, false);
		assertEquals(6, testReport.getNumErrors());
	}

	public void executeRun(final String filename, final boolean writeSuccess) throws URISyntaxException {
		URL url = getClass().getResource(filename);
		assertNotNull(url);
		final File f = new File(url.toURI());

		final ResourceProvider resourceManager = new TestFileResourceProvider(f);
		//testReport = new TestReport(new CsvResultFormatter());

		testReport = new StreamTestReport(new CsvResultFormatter(), new TestWriterDelegate(new StringWriter()), writeSuccess);
		tester = new ColumnPatternTester(new TestValidationLogImpl(ColumnPatternTester.class), resourceManager, testReport);

		tester.runTests();
	}

	static class TestFileResourceProvider implements ResourceProvider {

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
