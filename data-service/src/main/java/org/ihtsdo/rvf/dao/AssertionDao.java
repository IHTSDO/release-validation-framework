package org.ihtsdo.rvf.dao;

import org.ihtsdo.rvf.entity.Assertion;

import java.util.List;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 21:39
 */
public interface AssertionDao extends EntityDao<Assertion> {
    List<Assertion> findAll();
}
