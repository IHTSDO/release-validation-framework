package org.ihtsdo.rvf.repository;

import org.ihtsdo.rvf.entity.AssertionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssertionGroupRepository extends JpaRepository<AssertionGroup, Long> {
	AssertionGroup findByName(String groupName);
}
