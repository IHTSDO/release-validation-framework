package org.ihtsdo.rvf.core.data.repository;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.AssertionTest;
import org.ihtsdo.rvf.core.data.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AssertionTestRepository extends JpaRepository<AssertionTest, Long>{

	List<AssertionTest> findAssertionTestsByAssertion(Assertion assertion);

	AssertionTest findByAssertionAndTest(Assertion assertion, Test test);
}
