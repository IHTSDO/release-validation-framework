package org.ihtsdo.rvf.dao;

import java.io.Serializable;

public interface EntityDao<T> {

	void save(T entity);

	T load(Serializable id);

	void delete(T entity);

}
