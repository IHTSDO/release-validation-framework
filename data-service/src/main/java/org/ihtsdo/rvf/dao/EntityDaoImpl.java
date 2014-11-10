package org.ihtsdo.rvf.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public class EntityDaoImpl<T> implements EntityDao<T> {

	private Class<T> type;

	@Autowired
	private SessionFactory sessionFactory;

    protected EntityDaoImpl() {
    }

    protected EntityDaoImpl(Class<T> type) {
		this.type = type;
	}

	@Override
	public void save(T entity) {
		getCurrentSession().save(entity);
	}

	@Override
	public T load(Class clazz, Serializable id) {
		return (T) getCurrentSession().get(clazz, id);
	}

	@Override
	public void delete(T entity) {
        getCurrentSession().delete(entity);
	}

    @Override
    public List<T> findAll(Class clazz){
        return getCurrentSession().createCriteria(clazz).list();
    }

    @Override
    public Long count(Class clazz){
        return (Long) getCurrentSession().createCriteria(clazz).setProjection(Projections.rowCount()).uniqueResult();
    }

	@Override
    public Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}
