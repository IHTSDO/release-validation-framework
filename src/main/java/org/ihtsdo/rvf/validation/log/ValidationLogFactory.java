package org.ihtsdo.rvf.validation.log;

public interface ValidationLogFactory {

	ValidationLog getValidationLog(Class<?> subject);

}
