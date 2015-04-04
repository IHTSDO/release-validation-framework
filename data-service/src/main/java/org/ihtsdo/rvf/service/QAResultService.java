package org.ihtsdo.rvf.service;

import java.util.List;

public interface QAResultService {

	List<String> getResult(Long runId, String assertionUUID);

}
