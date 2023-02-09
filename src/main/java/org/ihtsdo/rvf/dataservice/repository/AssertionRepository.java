package org.ihtsdo.rvf.dataservice.repository;

import org.ihtsdo.rvf.entity.Assertion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AssertionRepository extends JpaRepository<Assertion, Long> {

	Assertion findByUuid(String uuid);

	List<Assertion> findAssertionsByKeywords(String keyWords);
	
	Assertion findByAssertionId(Long assertionId);
}
