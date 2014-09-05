package org.ihtsdo.rvf.service;

public interface EntityService<T> {

	void update(T entity);

	void delete(T entity);

}
