package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.dao.EntityDao;

import java.util.List;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 16:12
 */
public class EntityServiceImpl<T> implements EntityService<T> {

    public EntityServiceImpl(EntityDao dao) {
        this.dao = dao;
    }

    @Override
    public void update(T entity) {

    }

    @Override
    public void delete(T entity) {

    }

    @Override
    public List<T> findAll() {
        return null;
    }

    @Override
    public T find(Long id) {
        return null;
    }

    private final EntityDao dao;
}
