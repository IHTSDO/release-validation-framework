package org.ihtsdo.rvf.execution.service.util;

import static org.junit.Assert.*;
import static org.ihtsdo.rvf.execution.service.util.RvfReleaseDbSchemaNameGenerator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RvfReleaseDbSchemaNameGeneratorTest {
	@Test
	public void testInternationalMemberRelase() {
		assertEquals("rvf_intrf2_member_20180731t120000z", generate("SnomedCT_InternationalRF2_MEMBER_20180731T120000Z.zip"));
	}

	
	@Test
	public void testInternationalAlphaRelase() {
		assertEquals("rvf_intrf2_alpha_20180731t120000z", generate("xSnomedCT_InternationalRF2_ALPHA_20180731T120000Z.zip"));
	}
	
	@Test
	public void testInternationalProductionRelase() {
		assertEquals("rvf_intrf2_prod_20180731t120000z", generate("SnomedCT_InternationalRF2_PRODUCTION_20180731T120000Z.zip"));
	}
	
	@Test
	public void testTermServerMainExport() {
		assertEquals("rvf_prod_main_20180131_201802020832", generate("prod_main_20180131_201802020832.zip"));
	}
	
	@Test
	public void testTermServerExportForExtension() {
		assertEquals("rvf_prodms_main_20180731_usfix_20180901_20180817104834", generate("prod-ms_main_2018-07-31_SNOMEDCT-USFIX_2018-09-01_20180817104834.zip"));
	}
	
	@Test
	public void testExtensionRelease() {
		assertEquals("rvf_usextensionrf2_prod_20180301t120000z", generate("SnomedCT_USExtensionRF2_PRODUCTION_20180301T120000Z.zip"));
	}
}
