package org.ihtsdo.rvf.repository;

import java.util.List;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionTest;
import org.ihtsdo.rvf.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AssertionTestRepository extends JpaRepository<AssertionTest, Long>{

	List<AssertionTest> findAssertionTestsByAssertion(Assertion assertion);

	AssertionTest findByAssertionAndTest(Assertion assertion, Test test);
}
