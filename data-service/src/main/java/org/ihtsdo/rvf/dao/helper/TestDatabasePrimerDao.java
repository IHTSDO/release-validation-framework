package org.ihtsdo.rvf.dao.helper;

import org.hibernate.SessionFactory;
import org.ihtsdo.rvf.helper.TestEntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;

public class TestDatabasePrimerDao extends TestEntityGenerator {

	@Autowired
	private SessionFactory sessionFactory;

}
