package org.ihtsdo.rvf.dao;

import java.util.List;
import java.util.UUID;

public interface QAResultDao {

	List<String> getResultDetails(Long runId, UUID assertionUUID);

}
