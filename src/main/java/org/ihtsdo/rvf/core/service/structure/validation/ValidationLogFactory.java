package org.ihtsdo.rvf.core.service.structure.validation;

public interface ValidationLogFactory {

	ValidationLog getValidationLog(Class<?> subject);

}
