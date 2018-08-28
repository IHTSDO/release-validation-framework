package org.ihtsdo.rvf.dao;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityDaoImpl<T> implements EntityDao<T> {

	private Class<T> type;

	@Autowired
	private SessionFactory sessionFactory;

	protected EntityDaoImpl() {
	}

	protected EntityDaoImpl(final Class<T> type) {
		this.type = type;
	}

	@Override
	public T save(final T entity) {
		getCurrentSession().save(entity);
		return entity;
	}

	@Override
	public T update(final T entity) {
		try {
			final Object merged = getCurrentSession().merge(entity);
			getCurrentSession().update(merged);
			return (T) merged;
		}
		catch (final ObjectNotFoundException e) {
			// disappeared already due to cascade
			return entity;
		}
	}

	@Override
	public T load(final Class clazz, final Serializable id) {
		return (T) getCurrentSession().get(clazz, id);
	}

	@Override
	public T findByUuid(final Class clazz, final UUID uuid) {

		return (T) getCurrentSession().createCriteria(clazz).add(Restrictions.eq("uuid", uuid.toString())).uniqueResult();
	}

	@Override
	public void delete(final T entity) {
		getCurrentSession().delete(entity);
	}

	@Override
	public List<T> findAll(final Class clazz){
		return getCurrentSession().createCriteria(clazz).list();
	}

	@Override
	public Long count(final Class clazz){
		return (Long) getCurrentSession().createCriteria(clazz).setProjection(Projections.rowCount()).uniqueResult();
	}

	@Override
	public Session getCurrentSession() { return sessionFactory.getCurrentSession();
	}

}
