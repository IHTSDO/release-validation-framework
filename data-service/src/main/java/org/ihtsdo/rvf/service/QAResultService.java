package org.ihtsdo.rvf.service;

import java.util.List;
import java.util.UUID;

public interface QAResultService {

	List<String> getResult(Long runId, UUID assertionUUID);

}
