package org.ihtsdo.rvf.dao;

import org.hibernate.Session;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface EntityDao<T> {

	T save(T entity);

	T update(T entity);

	T load(Class clazz, Serializable id);

	T findByUuid(Class clazz, UUID uuid);

	void delete(T entity);

	List<T> findAll(Class clazz);

	Long count(Class clazz);

	Session getCurrentSession();
}
