package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AssertionServiceImpl extends EntityServiceImpl<Assertion> implements AssertionService {

	@Autowired
	private AssertionDao assertionDao;

	@Autowired
	public AssertionServiceImpl(AssertionDao dao) {
		super(dao);
	}

	@Override
	public Assertion create(String name, Map<String, String> properties) {
		Assertion assertion = new Assertion();
		setProperties(assertion, properties);
		assertionDao.save(assertion);
		return assertion;
	}

	@Override
	public Assertion update(Long id, Map<String, String> newValues) {
		Assertion assertion = find(id);
		setProperties(assertion, newValues);
		assertionDao.save(assertion);
		return assertion;
	}

	@Override
	public List<Assertion> findAll() {
		return assertionDao.findAll();
	}

	@Override
	public Assertion find(Long id) {
		return assertionDao.load(id);
	}

	// todo use beanUtils/propertyUtils reflection for each of the properties
	private void setProperties(Assertion assertion, Map<String, String> properties) {
		assertion.setName(properties.get("name"));
		assertion.setDescription(properties.get("description"));
		assertion.setDocLink(properties.get("docLink"));
		assertion.setKeywords(properties.get("keywords"));
		assertion.setStatement(properties.get("statement"));
	}

}
