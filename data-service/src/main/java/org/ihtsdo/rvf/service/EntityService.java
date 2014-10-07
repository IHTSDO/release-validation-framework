package org.ihtsdo.rvf.service;

import java.util.List;

public interface EntityService<T> {

	T update(T entity);

	void delete(T entity);

    T create(T entity);

    List<T> findAll(T entity);
}
