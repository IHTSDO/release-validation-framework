package org.ihtsdo.rvf.dao;

import java.util.List;

public interface QAResultDao {

	List<String> getResultDetails(Long runId, String assertionUUID);

}
