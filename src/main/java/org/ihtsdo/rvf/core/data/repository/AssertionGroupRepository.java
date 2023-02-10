package org.ihtsdo.rvf.core.data.repository;

import org.ihtsdo.rvf.core.data.model.AssertionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssertionGroupRepository extends JpaRepository<AssertionGroup, Long> {
	AssertionGroup findByName(String groupName);
}
