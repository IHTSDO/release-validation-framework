package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.entity.Assertion;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 21:05
 */
public interface AssertionService extends EntityService<Assertion> {

    Assertion create(String name);

}
