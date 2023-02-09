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

import org.ihtsdo.rvf.validation.impl.CsvMetadataResultFormatter;
import org.ihtsdo.rvf.validation.impl.StreamTestReport;
import org.ihtsdo.rvf.validation.log.impl.TestValidationLogImpl;
import org.ihtsdo.rvf.validation.resource.ResourceProvider;
import org.junit.Test;

public class RF2FileStructureTesterTest {
	private RF2FileStructureTester rf2FileStructureTester;
	private StreamTestReport testReport;

	@Test
	public void testFileWithLFOnlyAsLineTerminator() throws URISyntaxException {
		executeRun("/rel2_sRefset_SimpleMapDelta_WithUnixLineEnding.txt", false);
		assertEquals("Total errors not matching", 2, testReport.getNumErrors());
		System.out.println(testReport.getResult());
	}

	
	@Test
	public void testFileWithCRLFAsLineTerminator() throws Exception {
		executeRun("/der2_Refset_SimpleDelta_INT_20140131.txt", false);
		assertEquals("Total errors not matching", 0, testReport.getNumErrors());
	}

	@Test
	public void testFileWithoutLineTerminatorForLastLine() throws Exception {
		executeRun("/der2_Refset_SimpleDelta_LastLineWithoutTerminator.txt", false);
		assertEquals("Total errors not matching", 1, testReport.getNumErrors());
	}
	
	@Test
	public void testEmptyFIle() throws Exception {
		executeRun("/rel2_Refset_SimpleDelta_INT_20140131_Empty.txt", false);
		assertEquals("Total errors not matching", 1, testReport.getNumErrors());
	}
	
	
	private void executeRun(final String filename, final boolean writeSucceses) throws URISyntaxException {
		final File f = new File(getClass().getResource(filename).toURI());
		final ResourceProvider resourceManager = new TestFileResourceProvider(f);
		testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), writeSucceses);
		rf2FileStructureTester = new RF2FileStructureTester(new TestValidationLogImpl(RF2FileStructureTester.class), resourceManager, testReport);
		rf2FileStructureTester.runTests();
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
		public boolean match(String name) {
			return false;
		}
	}
}
