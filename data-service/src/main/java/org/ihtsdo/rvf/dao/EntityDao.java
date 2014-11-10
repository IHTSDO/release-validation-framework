package org.ihtsdo.rvf.dao;

import org.hibernate.Session;

import java.io.Serializable;
import java.util.List;

public interface EntityDao<T> {

	void save(T entity);

	T load(Class clazz, Serializable id);

	void delete(T entity);

    List<T> findAll(Class clazz);

    Long count(Class clazz);

    Session getCurrentSession();
}
