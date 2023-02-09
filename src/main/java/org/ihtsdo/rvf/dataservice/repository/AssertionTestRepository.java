package org.ihtsdo.rvf.dataservice.repository;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AssertionTestRepository extends JpaRepository<AssertionTest, Long>{

	List<AssertionTest> findAssertionTestsByAssertion(Assertion assertion);

	AssertionTest findByAssertionAndTest(Assertion assertion, Test test);
}
