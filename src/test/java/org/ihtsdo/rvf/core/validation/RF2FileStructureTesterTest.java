package org.ihtsdo.rvf.core.validation;

import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.ihtsdo.rvf.core.service.structure.validation.CsvMetadataResultFormatter;
import org.ihtsdo.rvf.core.service.structure.validation.RF2FileStructureTester;
import org.ihtsdo.rvf.core.service.structure.validation.StreamTestReport;
import org.ihtsdo.rvf.core.service.structure.validation.TestValidationLogImpl;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RF2FileStructureTesterTest {
	private StreamTestReport testReport;

	@org.junit.jupiter.api.Test
	public void testFileWithLFOnlyAsLineTerminator() throws URISyntaxException {
		executeRun("/rel2_sRefset_SimpleMapDelta_WithUnixLineEnding.txt");
		assertEquals(2, testReport.getNumErrors(), "Total errors not matching");
		System.out.println(testReport.getResult());
	}

	
	@Test
	public void testFileWithCRLFAsLineTerminator() throws Exception {
		executeRun("/der2_Refset_SimpleDelta_INT_20140131.txt");
		assertEquals(0, testReport.getNumErrors(), "Total errors not matching");
	}

	@Test
	public void testFileWithoutLineTerminatorForLastLine() throws Exception {
		executeRun("/der2_Refset_SimpleDelta_LastLineWithoutTerminator.txt");
		assertEquals(1, testReport.getNumErrors(), "Total errors not matching");
	}
	
	@Test
	public void testEmptyFIle() throws Exception {
		executeRun("/rel2_Refset_SimpleDelta_INT_20140131_Empty.txt");
		assertEquals(1, testReport.getNumErrors(), "Total errors not matching");
	}
	
	
	private void executeRun(final String filename) throws URISyntaxException {
		URL res = getClass().getResource(filename);
		assertNotNull(res);
		final File f = new File(res.toURI());
		final ResourceProvider resourceManager = new TestFileResourceProvider(f);
		testReport = new StreamTestReport(new CsvMetadataResultFormatter(), new TestWriterDelegate(new StringWriter()), false);
		RF2FileStructureTester rf2FileStructureTester = new RF2FileStructureTester(new TestValidationLogImpl(RF2FileStructureTester.class), resourceManager, testReport);
		rf2FileStructureTester.runTests();
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
		public boolean match(String name) {
			return false;
		}
	}
}
