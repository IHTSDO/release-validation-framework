package org.ihtsdo.rvf.dao;

import java.io.Serializable;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 21:35
 */
public interface EntityDao<T> {

    void save(T entity);

    T load(Serializable id);

    void delete(T entity);
}
