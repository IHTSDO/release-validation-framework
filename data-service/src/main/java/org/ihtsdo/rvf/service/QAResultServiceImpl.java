package org.ihtsdo.rvf.service;

import java.util.List;
import java.util.UUID;

import org.ihtsdo.rvf.dao.QAResultDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


@Transactional
public class QAResultServiceImpl implements QAResultService {

	@Autowired
	private QAResultDao resultDao;
	@Override
	public List<String> getResult(final Long runId, final UUID assertionUUID) {
		return resultDao.getResultDetails(runId, assertionUUID);
	}
}
