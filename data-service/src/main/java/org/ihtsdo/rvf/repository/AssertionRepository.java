package org.ihtsdo.rvf.repository;

import java.util.List;

import org.ihtsdo.rvf.entity.Assertion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AssertionRepository extends JpaRepository<Assertion, Long> {

	Assertion findByUuid(String uuid);

	List<Assertion> findAssertionsByKeywords(String keyWords);
	
	Assertion findByAssertionId(Long assertionId);
}
