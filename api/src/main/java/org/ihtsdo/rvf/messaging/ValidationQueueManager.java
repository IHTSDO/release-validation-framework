package org.ihtsdo.rvf.messaging;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.execution.service.ValidationReportService;
import org.ihtsdo.rvf.execution.service.ValidationReportService.State;
import org.ihtsdo.rvf.execution.service.config.ValidationJobResourceConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

@Service
public class ValidationQueueManager {

	private static final String FILES_TO_VALIDATE = "files_to_validate";
	private static final String FAILURE_MESSAGE = "failureMessage";
	
	@Autowired
	private JmsTemplate jmsTemplate;
	
	@Autowired
	private ValidationReportService reportService;
	
	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;
	
	@Autowired
	private ResourceLoader cloudResourceLoader;
	
	@Value("${rvf.execution.isAutoScalingEnabled}")
	private Boolean isAutoScalingEnabled;
	
	@Value("${rvf.validation.queue.name}")
	private String destinationName;
	
	private ResourceManager validationJobResourceManager;

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationQueueManager.class);

	@PostConstruct
	public void init() {
		validationJobResourceManager = new ResourceManager(jobResourceConfig, cloudResourceLoader);
	}

	public void queueValidationRequest(ValidationRunConfig config, Map<String, String> responseMap) {
		try {
			if (saveUploadedFiles(config, responseMap)) {
				Gson gson = new Gson();
				String configJson = gson.toJson(config);
				LOGGER.info("Send Jms message to queue for validation config json:" + configJson);
				jmsTemplate.convertAndSend(destinationName, configJson);
				reportService.writeState(State.QUEUED, config.getStorageLocation());
			}
		} catch (IOException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to save uploaded prospective release file due to " + e.getMessage());
		} catch (JmsException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to send queueing message due to " + e.getMessage());
		} catch (NoSuchAlgorithmException | DecoderException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to write Queued State to Storage Location due to " + e.getMessage());
		}
	}

	/*
	 * The issue here is that spring cleans up Multipart files when Dispatcher
	 * is complete, so we need to save off the file before we allow the parent
	 * thread to finish.
	 */
	private boolean saveUploadedFiles(final ValidationRunConfig config,
			final Map<String, String> responseMap) throws IOException, NoSuchAlgorithmException, DecoderException {
		
		if (!jobResourceConfig.isUseCloud() && config.isProspectiveFileInS3()) {
			responseMap.put(FAILURE_MESSAGE, "Can't process files from S3 as validation resource config is configured to use local files "
					+ jobResourceConfig.toString());
			reportService.writeState(State.FAILED, config.getStorageLocation());
			return false;
		}
		if (!config.isProspectiveFileInS3()) {
			String filename = config.getFile().getOriginalFilename();
			String jobStoragePath = config.getStorageLocation() + File.separator + FILES_TO_VALIDATE + File.separator;
			String targetFilePath = jobStoragePath + filename;
			validationJobResourceManager.writeResource(targetFilePath, config.getFile().getInputStream());
			config.setProspectiveFileFullPath(targetFilePath);
			config.setTestFileName(filename);
			if (config.getManifestFile() != null) {
				String manifestS3Path = jobStoragePath + config.getManifestFile().getOriginalFilename();
				validationJobResourceManager.writeResource(manifestS3Path, config.getManifestFile().getInputStream());
				config.setManifestFileFullPath(manifestS3Path);
			}
			config.setProspectiveFilesInS3(jobResourceConfig.isUseCloud());
			if (jobResourceConfig.isUseCloud()) {
				config.setBucketName(jobResourceConfig.getCloud().getBucketName());
			}
		} 
		return true;
	}
}
