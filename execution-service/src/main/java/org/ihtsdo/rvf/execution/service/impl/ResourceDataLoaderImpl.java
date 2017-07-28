package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ResourceDataLoader;
import org.ihtsdo.rvf.execution.service.util.RvfDynamicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.S3Object;

@Service
public class ResourceDataLoaderImpl implements ResourceDataLoader {
	private static final String US_TO_GB_TERMS_MAP_FILENAME = "us-to-gb-terms-map.txt";
	private static final String UTF_8 = "UTF-8";
	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;
	private String validationResourcePath;

	public String getValidationResourcePath() {
		return validationResourcePath;
	}

	public void setValidationResourcePath(String validationResourcePath) {
		this.validationResourcePath = validationResourcePath;
	}

	@Resource
	private S3Client s3Client;
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDataLoaderImpl.class);
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
			LOGGER.info("validationResourcePath:" + validationResourcePath);
			int index = validationResourcePath.indexOf("/");
			String bucketname = validationResourcePath.substring(0,index);
			LOGGER.info("The bucket name extracted from validationResourcePath:" + bucketname);
			String prefix = validationResourcePath.substring(index);
			LOGGER.info("The prefix extracted from validationResourcePath:" + prefix);
			S3Object termFileObj = s3Client.getObject(bucketname, prefix + US_TO_GB_TERMS_MAP_FILENAME);
			LOGGER.info("External configuration file {} found at {}", US_TO_GB_TERMS_MAP_FILENAME, validationResourcePath);
			File localMapFile = new File (localResourceDir, US_TO_GB_TERMS_MAP_FILENAME);
			try (InputStream input = termFileObj.getObjectContent();
				OutputStream out = new FileOutputStream(localMapFile);) {
				IOUtils.copy(input, out);
			}
		} catch (Throwable  t) {
			final String errorMsg = "Error when trying to download the us-to-gb-terms-map.txt file from S3 at path:" + validationResourcePath;
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
			LOGGER.info("No external configuration file {} found at {} therefore the default will be used", US_TO_GB_TERMS_MAP_FILENAME, validationResourcePath);
			try {
				copyDefaultDataFiles(localResourceDir, US_TO_GB_TERMS_MAP_FILENAME);
			} catch (IOException e) {
				final String errorMsg = "Error when trying to copy default us-to-gb-terms-map.txt file.";
				LOGGER.error(errorMsg, e);
				throw new BusinessServiceException(errorMsg, e);
			}
		}
	}
	
	@Override
	public void loadResourceData(final String schemaName) throws BusinessServiceException {
		init();
		if (schemaName != null) {
			try (final Connection connection = rvfDynamicDataSource.getConnection(schemaName)) {
				final ScriptRunner runner = new ScriptRunner(connection);
				try (InputStream input = getClass().getResourceAsStream("/sql/create-resource-tables.sql")) {
					runner.runScript(new InputStreamReader(input));
				} 
				final File temp = File.createTempFile("load-resource-data.txt", null);
				try {
					try (final InputStream input = getClass().getResourceAsStream("/sql/load-resource-data.sql")) {
						for (String line : IOUtils.readLines(input)) {
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
