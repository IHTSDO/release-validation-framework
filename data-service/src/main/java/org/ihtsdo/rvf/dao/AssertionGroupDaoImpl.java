//package org.ihtsdo.rvf.dao;
//
//import java.util.List;
//
//import org.ihtsdo.rvf.entity.AssertionGroup;
//import org.springframework.stereotype.Repository;
//@Repository
//public class AssertionGroupDaoImpl extends EntityDaoImpl<AssertionGroup> implements AssertionGroupDao{
//
//	public AssertionGroupDaoImpl() {
//		super(AssertionGroup.class);
//	}
//	@SuppressWarnings("unchecked")
//	@Override
//	public List<AssertionGroup> findAll() {
//		return getCurrentSession()
//				.createQuery("from AssertionGroup ag order by ag.id").list();
//	}
//	@Override
//	public AssertionGroup create(AssertionGroup group) {
//		save(group);
//		return group;
//	}
//}
