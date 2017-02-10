package org.ihtsdo.rvf.dao;

import java.util.List;

import org.ihtsdo.rvf.entity.AssertionGroup;

public interface AssertionGroupDao {
	List<AssertionGroup> findAll();
	AssertionGroup create(AssertionGroup group);
	AssertionGroup update(AssertionGroup group);
}
