package org.ihtsdo.rvf.execution.service;

import java.util.UUID;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;

public interface ResultExtractorService {

	String extractResultToJson(Long runId, UUID assertionUUID) throws BusinessServiceException;

}
