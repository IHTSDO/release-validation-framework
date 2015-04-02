package org.ihtsdo.rvf.dao;

import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.rvf.entity.QAResult;

public class QAResultDaoImpl extends  EntityDaoImpl<QAResult> implements QAResultDao {

	@Override
	public List<String> getResultDetails(final Long runId, final String assertionUUID) {
		
		@SuppressWarnings("unchecked")
		final List<QAResult> results = getCurrentSession()
				.createQuery("from QAResult result where result.assertion.uuid=:assertionUUID and result.runId =:runID")
				.setParameter("assertionUUID", assertionUUID)
				.setParameter("runID",runId)
				.list();
		
		if (results == null) {
			return null;
		}
		final List<String> details = new ArrayList<>();
		for (final QAResult result : results) {
			details.add(result.getDetails());
		}
		return details;
	}

}
