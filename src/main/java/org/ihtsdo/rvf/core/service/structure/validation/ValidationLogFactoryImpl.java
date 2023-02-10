package org.ihtsdo.rvf.core.service.structure.validation;

import org.springframework.stereotype.Service;

@Service
public class ValidationLogFactoryImpl implements ValidationLogFactory {

	@Override
	public ValidationLog getValidationLog(Class<?> subject) {
		return new ValidationLogImpl(subject);
	}

}
