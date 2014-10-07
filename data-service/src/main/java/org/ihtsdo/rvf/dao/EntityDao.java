package org.ihtsdo.rvf.dao;

import java.io.Serializable;
import java.util.List;

public interface EntityDao<T> {

	void save(T entity);

	T load(Serializable id);

	void delete(T entity);

    List<T> findAll(T entity);
}
