package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.dao.EntityDao;

public class EntityServiceImpl<T> implements EntityService<T> {

	private final EntityDao dao;

	public EntityServiceImpl(EntityDao dao) {
		this.dao = dao;
	}

	@Override
	public void update(T entity) {

	}

	@Override
	public void delete(T entity) {

	}

}
