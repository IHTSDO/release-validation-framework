package org.ihtsdo.release.assertion;

import org.ihtsdo.release.assertion._1_0.ColumnPatternTestConfiguration;
import org.ihtsdo.release.assertion.log.TestValidationLogImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class ColumnPatternTestTest {

	private ColumnPatternTest columnPatternTest;
	private TestValidationLogImpl testValidationLog;
	private ColumnPatternTestConfiguration configuration;

	@Before
	public void setup() {
		testValidationLog = new TestValidationLogImpl(ColumnPatternTest.class);
		configuration = new ColumnPatternTestConfiguration();
		columnPatternTest = new ColumnPatternTest(testValidationLog, configuration, new File(""));
	}

	@Test
	public void testMissingFileName() {
		ColumnPatternTestConfiguration.File file = new ColumnPatternTestConfiguration.File();
		ColumnPatternTestConfiguration.File.Column column = new ColumnPatternTestConfiguration.File.Column();
		column.setName("id");
		column.setSctid(""); // is an SCTID
		file.getColumn().add(column);
		configuration.getFile().add(file);

		columnPatternTest.runTests();

		Assert.assertEquals(1, testValidationLog.getErrorsAndArgumentsMap().size());
	}

}
