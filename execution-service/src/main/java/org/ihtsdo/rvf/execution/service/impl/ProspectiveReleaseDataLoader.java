package org.ihtsdo.rvf.execution.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;

public class ProspectiveReleaseDataLoader {

	private static final String SNAPSHOT_TABLE = "%_s";
	private ValidationRunConfig validationConfig;
	private ReleaseDataManager releaseDataManager;

	public ProspectiveReleaseDataLoader(ValidationRunConfig validationConfig, ReleaseDataManager releaseDataManager) {
		this.validationConfig = validationConfig;
		this.releaseDataManager = releaseDataManager;
	}

	public List<String> loadProspectiveDeltaWithPreviousSnapshotIntoDB(String prospectiveVersion) throws BusinessServiceException {
		List<String> filesLoaded = new ArrayList<>();
		if (validationConfig.isRf2DeltaOnly()) {
			releaseDataManager.loadSnomedData(prospectiveVersion, filesLoaded, validationConfig.getLocalProspectiveFile());
			//copy snapshot from previous release
			copySnapshotFromPreviousRelease(validationConfig.getPrevIntReleaseVersion(),prospectiveVersion);
			addDeltaToSnapshot(prospectiveVersion);
		}
		return filesLoaded;
	}

	private void addDeltaToSnapshot(String prospectiveVersion) {
		releaseDataManager.updateSnapshotTableWithDataFromDelta(prospectiveVersion);
	}

	private void copySnapshotFromPreviousRelease(String prevIntReleaseVersion,String prospectiveVersion) {
		releaseDataManager.copyTableData(prevIntReleaseVersion, prospectiveVersion,SNAPSHOT_TABLE);
	}

	public ValidationRunConfig getValidationConfig() {
		return validationConfig;
	}

	public void setValidationConfig(ValidationRunConfig validationConfig) {
		this.validationConfig = validationConfig;
	}
}
