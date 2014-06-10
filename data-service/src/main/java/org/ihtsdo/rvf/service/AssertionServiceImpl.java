package org.ihtsdo.rvf.service;

import org.ihtsdo.rvf.dao.AssertionDao;
import org.ihtsdo.rvf.dao.EntityDao;
import org.ihtsdo.rvf.entity.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AssertionServiceImpl extends EntityServiceImpl<Assertion> implements AssertionService {

    @Autowired
    public AssertionServiceImpl(AssertionDao dao) {
        super(dao);
    }


    @Autowired
    private AssertionDao assertionDao;
}
