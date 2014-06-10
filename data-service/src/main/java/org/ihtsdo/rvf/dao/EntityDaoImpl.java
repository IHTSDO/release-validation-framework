package org.ihtsdo.rvf.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 21:37
 */
@Repository
public abstract class EntityDaoImpl<T> implements EntityDao<T> {
    @Override
    public void save(T entity) {

    }

    @Override
    public T load(Serializable id) {
        return null;
    }

    @Override
    public void delete(T entity) {

    }

    protected Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Autowired
    private SessionFactory sessionFactory;
}
