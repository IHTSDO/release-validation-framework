package org.ihtsdo.rvf.execution.service;

import java.io.IOException;
import java.sql.SQLException;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;

public interface ResourceDataLoader {

	void loadResourceData(String schemaName) throws SQLException, IOException, BusinessServiceException;
}
