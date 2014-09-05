package org.ihtsdo.rvf.dao;

import org.ihtsdo.rvf.entity.Assertion;

import java.util.List;

public class AssertionDaoImpl extends EntityDaoImpl<Assertion> implements AssertionDao {

	public AssertionDaoImpl() {
		super(Assertion.class);
	}

	@Override
	public List<Assertion> findAll() {
		return getCurrentSession()
				.createQuery("from Assertion assertion order by assertion.name")
				.list();
	}

}
