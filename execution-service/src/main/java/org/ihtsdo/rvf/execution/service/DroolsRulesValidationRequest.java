package org.ihtsdo.rvf.execution.service;

import java.util.List;

public class DroolsRulesValidationRequest {
	
	private String effectiveTime;
	
	private boolean releaseAsAnEdition;
	
	private String includedModules;
	
	private List<String> droolsRulesGroupList;

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public boolean isReleaseAsAnEdition() {
		return releaseAsAnEdition;
	}

	public void setReleaseAsAnEdition(boolean releaseAsAnEdition) {
		this.releaseAsAnEdition = releaseAsAnEdition;
	}

	public String getIncludedModules() {
		return includedModules;
	}

	public void setIncludedModules(String includedModules) {
		this.includedModules = includedModules;
	}

	public List<String> getDroolsRulesGroupList() {
		return droolsRulesGroupList;
	}

	public void setDroolsRulesGroupList(List<String> droolsRulesGroupList) {
		this.droolsRulesGroupList = droolsRulesGroupList;
	}
	
}
