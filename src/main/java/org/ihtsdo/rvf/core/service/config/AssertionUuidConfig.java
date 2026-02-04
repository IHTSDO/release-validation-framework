package org.ihtsdo.rvf.core.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AssertionUuidConfig {

	@Value("${rvf.assertion.uuids.spanish-extension-exclude}")
	private String spanishExtensionExclude;

	@Value("${rvf.assertion.uuids.snapshot-exclude}")
	private String snapshotExclude;

	@Value("${rvf.assertion.uuids.snomed-rt-identifier}")
	private String snomedRtIdentifier;

	@Value("${rvf.assertion.uuids.us-authoring-exclude}")
	private String usAuthoringExclude;

	@Value("${rvf.assertion.uuids.au-authoring-exclude}")
	private String auAuthoringExclude;

	@Value("${rvf.assertion.uuids.us-edition-exclude}")
	private String usEditionExclude;

	@Value("${rvf.assertion.uuids.int-authoring-exclude}")
	private String intAuthoringExclude;

	@Value("${rvf.assertion.uuids.int-authoring-include}")
	private String intAuthoringInclude;

	@Value("${rvf.assertion.uuids.ms-authoring-include}")
	private String msAuthoringInclude;

	@Value("${rvf.assertion.uuids.int-edition-exclude}")
	private String intEditionExclude;

	@Value("${rvf.assertion.uuids.first-time-common-additional-exclude}")
	private String firstTimeCommonAdditionalExclude;

	@Value("${rvf.assertion.uuids.stated-relationship}")
	private String statedRelationship;

	@Value("${rvf.assertion.uuids.common-language-refsets}")
	private String commonLanguageRefsets;

	@Value("${rvf.assertion.uuids.mrcm-refsets}")
	private String mrcmRefsets;

	@Value("${rvf.assertion.uuids.common-refsets}")
	private String commonRefsets;

	public List<String> getSpanishExtensionExclude() {
		return Arrays.asList(spanishExtensionExclude.split(","));
	}

	public List<String> getSnapshotExclude() {
		return Arrays.asList(snapshotExclude.split(","));
	}

	public List<String> getSnomedRtIdentifier() {
		return Arrays.asList(snomedRtIdentifier.split(","));
	}

	public List<String> getUsAuthoringExclude() {
		return Arrays.asList(usAuthoringExclude.split(","));
	}

	public List<String> getAuAuthoringExclude() {
		return Arrays.asList(auAuthoringExclude.split(","));
	}

	public List<String> getUsEditionExclude() {
		return Arrays.asList(usEditionExclude.split(","));
	}

	public List<String> getIntAuthoringExclude() {
		return Arrays.asList(intAuthoringExclude.split(","));
	}

	public List<String> getIntAuthoringInclude() {
		return Arrays.asList(intAuthoringInclude.split(","));
	}

	public List<String> getMsAuthoringInclude() {
		return Arrays.asList(msAuthoringInclude.split(","));
	}

	public List<String> getIntEditionExclude() {
		return Arrays.asList(intEditionExclude.split(","));
	}

	public List<String> getFirstTimeCommonAdditionalExclude() {
		return Arrays.asList(firstTimeCommonAdditionalExclude.split(","));
	}

	public List<String> getStatedRelationship() {
		return Arrays.asList(statedRelationship.split(","));
	}

	public List<String> getCommonLanguageRefsets() {
		return Arrays.asList(commonLanguageRefsets.split(","));
	}

	public List<String> getMrcmRefsets() {
		return Arrays.asList(mrcmRefsets.split(","));
	}

	public List<String> getCommonRefsets() {
		return Arrays.asList(commonRefsets.split(","));
	}
}
