package org.ihtsdo.rvf.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

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
	public T save(T entity) {
		return (T) getCurrentSession().save(entity);
	}

	@Override
	public T load(Class clazz, Serializable id) {
		return (T) getCurrentSession().get(clazz, id);
	}

	@Override
	public T findByUuid(Class clazz, UUID uuid) {

        return (T) getCurrentSession().createCriteria(clazz).add(Restrictions.eq("uuid", uuid)).uniqueResult();
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
