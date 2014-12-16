package org.ihtsdo.rvf.validation.log.impl;

import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.ihtsdo.rvf.validation.log.ValidationLogFactory;

public class ValidationLogFactoryImpl implements ValidationLogFactory {

	@Override
	public ValidationLog getValidationLog(Class<?> subject) {
		return new ValidationLogImpl(subject);
	}

}
