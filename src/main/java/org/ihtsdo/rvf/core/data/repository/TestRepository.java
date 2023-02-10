package org.ihtsdo.rvf.core.data.repository;

import org.ihtsdo.rvf.core.data.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
}
