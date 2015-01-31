package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.entity.ReleaseCenter;

import java.util.List;
import java.util.UUID;

public interface EntityService<T> {

	T update(T entity);

	void delete(T entity);

    T create(T entity);

    List<T> findAll(Class clazz);

    T find(Class clazz, Long id);

    T find(Class clazz, UUID uuid);

    Long count(Class clazz);

    ReleaseCenter getIhtsdo();
}
