package org.ihtsdo.rvf.core.service.harness;

import java.io.File;
import java.util.List;

import org.ihtsdo.rvf.TestConfig;
import org.ihtsdo.rvf.core.service.ValidationRunner;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class ValidationRunnerHarnessForTest {
	@Autowired
	ValidationRunner runner;
	
	@Test
	public void testExtensionReleaseValidation () {
		ValidationRunConfig validationConfig = new ValidationRunConfig();
		validationConfig.setGroupsList(List.of("dk_authoring"));
		validationConfig.setExtensionDependency("int_20160131");
		validationConfig.setPreviousRelease("dk_20160215");
		validationConfig.setProspectiveFilesInS3(false);
		validationConfig.setProspectiveFileFullPath("SnomedCT_Release_DK1000005_20160731-DeltaOnly.zip");
		//local file will be deleted after test
		File localFile = new File("/Users/Releases/SnomedCT_Release_DK1000005_20160731-DeltaOnly.zip");
		validationConfig.setLocalProspectiveFile(localFile);
		validationConfig.setRf2DeltaOnly(true);
		validationConfig.setRunId(System.currentTimeMillis());
		runner.run(validationConfig);
	}

}
