package org.ihtsdo.rvf.core.data.repository;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AssertionRepository extends JpaRepository<Assertion, Long> {

	Assertion findByUuid(String uuid);

	List<Assertion> findAssertionsByKeywords(String keyWords);
	
	Assertion findByAssertionId(Long assertionId);
}
