package org.ihtsdo.rvf.dao.helper;

import org.hibernate.SessionFactory;
import org.ihtsdo.rvf.helper.TestEntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: Bronwen Cassidy
 * Date: 08/06/2014
 * Time: 21:49
 */
public class TestDatabasePrimerDao extends TestEntityGenerator {

    @Autowired
    private SessionFactory sessionFactory;
}
