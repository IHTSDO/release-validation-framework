package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.util.RvfDynamicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class ResourceDataLoaderImpl implements ResourceDataLoader {
	private static final String UTF_8 = "UTF-8";
	@Autowired
	RvfDynamicDataSource rvfDynamicDataSource;
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDataLoaderImpl.class);
	
	@Override
	public void loadResourceData(final String schemaName) throws BusinessServiceException {
		if (schemaName != null) {
			try (final Connection connection = rvfDynamicDataSource.getConnection(schemaName)) {
				final ScriptRunner runner = new ScriptRunner(connection);
				try (InputStream input = getClass().getResourceAsStream("/sql/create-resource-tables.sql")) {

					runner.runScript(new InputStreamReader(input));
				} 
				
				final File temp = File.createTempFile("load-resource-data.txt", null);
				final File tempDataFolder = Files.createTempDirectory("dataFiles").toFile();
				try {
					final String[] dataFiles = {"cs_words.txt","usTerms.txt","gbTerms.txt","semanticTags.txt"};
					copyDataFiles(dataFiles,tempDataFolder);
					try (final InputStream input = getClass().getResourceAsStream("/sql/load-resource-data.sql")) {
						for (String line : IOUtils.readLines(input)) {
							// process line and add to output file
							line = line.replaceAll("<data_location>", tempDataFolder.getPath());
							FileUtils.writeStringToFile(temp, line + "\n",UTF_8, true);
						}
					}
					try (final InputStreamReader reader = new InputStreamReader(new FileInputStream(temp))) {
						runner.runScript(reader);
					}
				} finally {
					temp.delete();
					tempDataFolder.delete();
				}
			} catch (final SQLException | IOException e) {
				final String errorMsg = "Error when loadding resource data to schema:" + schemaName;
				LOGGER.error(errorMsg, e);
				throw new BusinessServiceException(errorMsg, e);
			}
		}
	}

	private void copyDataFiles(final String[] resourceFileNames, final File tempDataFolder ) throws IOException {
		for( final String fileName : resourceFileNames) {
			try (final InputStream txtInput = getClass().getResourceAsStream("/datafiles/" + fileName);
					final Writer writer = new FileWriter(new File(tempDataFolder, fileName))) {
					IOUtils.copy(txtInput,writer,UTF_8);
			}
		}
			
	}
}
