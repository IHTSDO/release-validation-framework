package org.ihtsdo.rvf.execution.service;

import java.util.List;

public class RVFMysqlValidationRequest {

	private boolean isRf2DeltaOnly;
	
	private boolean writeSuccess;
	
	private List<String> groupsList;
	
	private Integer exportMax;
	
	public boolean isRf2DeltaOnly() {
		return this.isRf2DeltaOnly;
	}

	public boolean writeSucceses() {
		return this.writeSuccess;
	}

	public List<String> getGroupsList() {
		return this.groupsList;
	}

	public Integer getExportMax() {
		return this.exportMax;
	}

	public boolean isWriteSuccess() {
		return writeSuccess;
	}

	public void setWriteSuccess(boolean writeSuccess) {
		this.writeSuccess = writeSuccess;
	}

	public void setRf2DeltaOnly(boolean isRf2DeltaOnly) {
		this.isRf2DeltaOnly = isRf2DeltaOnly;
	}

	public void setGroupsList(List<String> groupsList) {
		this.groupsList = groupsList;
	}

	public void setExportMax(Integer exportMax) {
		this.exportMax = exportMax;
	}
}
