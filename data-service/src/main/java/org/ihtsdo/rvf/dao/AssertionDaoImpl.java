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

    @Override
    public List<Assertion> findAll() {
        Query query = getCurrentSession().createQuery(
                "select assertion " +
                        "from Assertion a " +
                        "order by a.id ");
        return query.list();
    }

}
