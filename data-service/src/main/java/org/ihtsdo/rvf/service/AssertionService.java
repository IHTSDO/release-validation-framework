package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.entity.Assertion;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface AssertionService extends EntityService<Assertion> {

    Assertion create(String name, Map<String, String> properties);
    Assertion update(Long id, Map<String, String> newValues);
    List<Assertion> findAll();
    Assertion find(Long id);
}
