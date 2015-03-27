package org.ihtsdo.rvf.execution.service;

import java.io.IOException;
import java.sql.SQLException;

public interface ResourceDataLoader {

	void loadResourceData(String schemaName) throws SQLException, IOException;

}
