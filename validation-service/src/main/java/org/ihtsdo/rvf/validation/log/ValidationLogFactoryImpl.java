package org.ihtsdo.rvf.validation.log;

public class ValidationLogFactoryImpl implements ValidationLogFactory {

	@Override
	public ValidationLog getValidationLog(Class<?> subject) {
		return new ValidationLogImpl(subject);
	}

}
