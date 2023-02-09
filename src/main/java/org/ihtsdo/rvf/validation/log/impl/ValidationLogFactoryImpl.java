package org.ihtsdo.rvf.validation.log.impl;

import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.ihtsdo.rvf.validation.log.ValidationLogFactory;
import org.springframework.stereotype.Service;

@Service
public class ValidationLogFactoryImpl implements ValidationLogFactory {

	@Override
	public ValidationLog getValidationLog(Class<?> subject) {
		return new ValidationLogImpl(subject);
	}

}
