package org.ihtsdo.rvf.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public abstract class EntityDaoImpl<T> implements EntityDao<T> {

	private final Class<T> type;

	@Autowired
	private SessionFactory sessionFactory;

	protected EntityDaoImpl(Class<T> type) {
		this.type = type;
	}

	@Override
	public void save(T entity) {
		getCurrentSession().save(entity);
	}

	@Override
	public T load(Serializable id) {
		return (T) getCurrentSession().get(type, id);
	}

	@Override
	public void delete(T entity) {
        getCurrentSession().delete(entity);
	}

    @Override
    public List<T> findAll(T entity){
        return getCurrentSession().createCriteria(type).list();
    }

	protected Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

}
