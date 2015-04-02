package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;

public interface ResultExtractorService {

	String extractResultToJson(Long runId, String assertionUUID) throws BusinessServiceException;

}
