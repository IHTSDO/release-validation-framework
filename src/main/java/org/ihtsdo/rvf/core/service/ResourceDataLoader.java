package org.ihtsdo.rvf.core.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.core.service.config.ValidationResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.dao.SimpleStorageResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class ResourceDataLoader {
	private static final String US_TO_GB_TERMS_MAP_FILENAME = "us-to-gb-terms-map.txt";
	private static final String UTF_8 = "UTF-8";
	
	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;
	
	@Autowired
	private ValidationResourceConfig testResourceConfig;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDataLoader.class);
	private File localResourceDir;

	
	private void init() throws BusinessServiceException {
		//check whether the resources are available from given path in S3
		// If not present then use the defaults from the resource folder instead.
		try {
			localResourceDir = Files.createTempDirectory("localData").toFile();
		} catch (IOException e) {
			final String errorMsg = "Error when creating local temp data dir";
			LOGGER.error(errorMsg, e);
			throw new BusinessServiceException(errorMsg, e);
		}
		try {
			S3Client s3Client = S3Client.builder().region(DefaultAwsRegionProviderChain.builder().build().getRegion()).build();
			ResourceManager resourceManager = new ResourceManager(testResourceConfig, new SimpleStorageResourceLoader(s3Client));
			File localMapFile = new File (localResourceDir, US_TO_GB_TERMS_MAP_FILENAME);
			try (InputStream input = resourceManager.readResourceStreamOrNullIfNotExists(US_TO_GB_TERMS_MAP_FILENAME);
					OutputStream out = new FileOutputStream(localMapFile)) {
				if (input != null) {
					IOUtils.copy(input, out);
				}
			}
		} catch (Throwable  t) {
			final String errorMsg = "Error when trying to download the us-to-gb-terms-map.txt file from S3 via :" +  testResourceConfig;
			LOGGER.error(errorMsg, t);
		} 
		boolean isExternalConfigFound = false;
		if (localResourceDir != null) {
			for (File file : localResourceDir.listFiles()) {
				if (file.getName().equals(US_TO_GB_TERMS_MAP_FILENAME)) {
					isExternalConfigFound = true;
					break;
				}
			}
		}
		if (!isExternalConfigFound) {
			LOGGER.info("No external configuration file {} found and the default file will be used", US_TO_GB_TERMS_MAP_FILENAME);
			try {
				copyDefaultDataFiles(localResourceDir, US_TO_GB_TERMS_MAP_FILENAME);
			} catch (IOException e) {
				final String errorMsg = "Error when trying to copy default us-to-gb-terms-map.txt file.";
				LOGGER.error(errorMsg, e);
				throw new BusinessServiceException(errorMsg, e);
			}
		}
	}
	
	public void loadResourceData(final String schemaName) throws BusinessServiceException {
		init();
		if (schemaName != null) {
			try (final Connection connection = rvfDynamicDataSource.getConnection(schemaName)) {
				final ScriptRunner runner = new ScriptRunner(connection);
				try (InputStreamReader inputReader = new InputStreamReader(getClass().getResourceAsStream("/sql/create-resource-tables.sql"))) {
					runner.runScript(inputReader);
				} 
				final File temp = File.createTempFile("load-resource-data.txt", null);
				try {
					try (final InputStream input = getClass().getResourceAsStream("/sql/load-resource-data.sql")) {
						for (String line : IOUtils.readLines(input, UTF_8)) {
							// process line and add to output file
							line = line.replaceAll("<data_location>", localResourceDir.getPath());
							FileUtils.writeStringToFile(temp, line + "\n",UTF_8, true);
						}
					}
					try (final InputStreamReader reader = new InputStreamReader(new FileInputStream(temp))) {
						runner.runScript(reader);
					}
				} finally {
					temp.delete();
					localResourceDir.delete();
				}
			} catch (final SQLException | IOException e) {
				final String errorMsg = "Error when loadding resource data to schema:" + schemaName;
				LOGGER.error(errorMsg, e);
				throw new BusinessServiceException(errorMsg, e);
			}
		}
	}

	private void copyDefaultDataFiles(final File tempDataFolder, final String... resourceFileNames) throws IOException {
		for( final String fileName : resourceFileNames) {
			try (final InputStream txtInput = getClass().getResourceAsStream("/datafiles/" + fileName);
					final Writer writer = new FileWriter(new File(tempDataFolder, fileName))) {
					IOUtils.copy(txtInput,writer,UTF_8);
			}
		}
	}
}
