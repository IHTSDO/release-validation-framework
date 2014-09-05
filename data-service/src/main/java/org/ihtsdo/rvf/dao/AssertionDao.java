package org.ihtsdo.rvf.dao;

import org.ihtsdo.rvf.entity.Assertion;

import java.util.List;

public interface AssertionDao extends EntityDao<Assertion> {

	List<Assertion> findAll();

}
