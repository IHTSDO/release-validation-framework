package org.ihtsdo.rvf.executionservice.traceability;

import java.util.Date;
import java.util.Set;

public class Activity {

	private String username;
	private String branch;
	private String highestPromotedBranch;
	private Date commitDate;
	private Date promotionDate;
	private ActivityType activityType;
	private Set<ConceptChange> conceptChanges;

	public String getUsername() {
		return username;
	}

	public String getBranch() {
		return branch;
	}

	public String getHighestPromotedBranch() {
		return highestPromotedBranch;
	}

	public Date getCommitDate() {
		return commitDate;
	}

	public Date getPromotionDate() {
		return promotionDate;
	}

	public ActivityType getActivityType() {
		return activityType;
	}

	public Set<ConceptChange> getConceptChanges() {
		return conceptChanges;
	}
}
