package org.ihtsdo.rvf.execution.service.traceability;

import java.util.Date;

public class Activity {

	private String username;
	private String branch;
	private String highestPromotedBranch;
	private Date commitDate;
	private Date promotionDate;
	private ActivityType activityType;

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
}
