package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.dao.EntityDao;

import java.util.List;

public class EntityServiceImpl<T> implements EntityService<T> {

	private final EntityDao dao;

	public EntityServiceImpl(EntityDao dao) {
		this.dao = dao;
	}

	@Override
	public T update(T entity) {
        dao.save(entity);
        return entity;
	}

	@Override
	public void delete(T entity) {
        dao.delete(entity);
	}

    @Override
    public T create(T entity) {
        dao.save(entity);
        return entity;
    }

    @Override
    public List<T> findAll(T entity) {
        return dao.findAll(entity);
    }
}
