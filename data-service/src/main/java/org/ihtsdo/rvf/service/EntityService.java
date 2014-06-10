package org.ihtsdo.rvf.service;

import java.util.List;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 15:53
 */
public interface EntityService<T> {

    void update(T entity);

    void delete(T entity);

    List<T> findAll();

    T find(Long id);

}
