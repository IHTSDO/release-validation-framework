package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
@Service
public class ResourceDataLoaderImpl implements ResourceDataLoader {
	
	@Resource(name = "snomedDataSource")
	private BasicDataSource snomedDataSource;
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDataLoaderImpl.class);
	
	@Override
	public void loadResourceData(final String schemaName) throws BusinessServiceException {
		if (schemaName != null) {
			final String  useStr = "use " + schemaName + ";";
			try (Connection connection = snomedDataSource.getConnection()) {
				final ScriptRunner runner = new ScriptRunner(connection);
				try (Statement statement = connection.createStatement()) {
					statement.execute(useStr);
				} 
				try (InputStream input = getClass().getResourceAsStream("/sql/create-resource-tables.sql")) {

					runner.runScript(new InputStreamReader(input));
				} 
				try (InputStream input = getClass().getResourceAsStream("/sql/load-resource-data.sql")) {

					final File temp = File.createTempFile("load-resource-data.txt", null);
					final String dataFileFullPath = getClass().getResource("/datafiles").getFile();
					for (String line : IOUtils.readLines(input)) {
						// process line and add to output file
						line = line.replaceAll("<data_location>", dataFileFullPath);
						FileUtils.writeStringToFile(temp, line + "\n", true);
					}
					try (final InputStreamReader reader = new InputStreamReader(new FileInputStream(temp))) {
						runner.runScript(reader);
					}
					finally {
						temp.delete();
					}
				}
			} catch (final SQLException | IOException e) {
				final String errorMsg = "Error when loadding resource data to schema:" + schemaName;
				LOGGER.error(errorMsg, e);
				throw new BusinessServiceException(errorMsg, e);
			}
		}
	}

	public void setSnomedDataSource(final BasicDataSource snomedDataSourceX) {
		snomedDataSource = snomedDataSourceX;
	}
}
