package org.ihtsdo.rvf.dao;

import org.hibernate.Query;
import org.ihtsdo.rvf.entity.Assertion;

import java.io.Serializable;
import java.util.List;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 21:41
 */
public class AssertionDaoImpl extends EntityDaoImpl<Assertion> implements AssertionDao {

    public AssertionDaoImpl() {
        super(Assertion.class);
    }

    @Override
    public List<Assertion> findAll() {
        return getCurrentSession()
                .createQuery("from Assertion assertion order by assertion.name")
                .list();
    }

}
