package org.ihtsdo.rvf.core.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import org.apache.commons.codec.DecoderException;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.core.service.ValidationReportService;
import org.ihtsdo.rvf.core.service.ValidationReportService.State;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusResponse;
import org.ihtsdo.rvf.core.service.config.ValidationJobResourceConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
public class ValidationQueueManager {

	private static final String FILES_TO_VALIDATE = "files_to_validate";
	private static final String FAILURE_MESSAGE = "failureMessage";

	public static final String QUEUE_SUFFIX_AUTHORING = ".authoring";
	public static final String QUEUE_SUFFIX_RELEASE = ".release";
	
	@Autowired
	private JmsTemplate jmsTemplate;
	
	@Autowired
	private ValidationReportService reportService;
	
	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;
	
	@Autowired
	private ResourceLoader cloudResourceLoader;
	
	@Value("${rvf.validation.queue.name}")
	private String destinationName;

	@Autowired
	private MessagingHelper messagingHelper;
	
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
				LOGGER.info("Send Jms message to queue for validation config {}", config.toString());
				updateRvfStateTo(config, State.QUEUED);
				String queueSuffix = "";
				if (config.getResponseQueue() != null) {
					// both response queues are hard-coded in authoring service and release service
					queueSuffix = config.getResponseQueue().contains("termserver-release-validation.response") ? QUEUE_SUFFIX_AUTHORING :
							(config.getResponseQueue().contains("srs.build-job-status") ? QUEUE_SUFFIX_RELEASE : "");
				}
				jmsTemplate.convertAndSend(destinationName + queueSuffix, configJson);
				reportService.writeState(State.QUEUED, config.getStorageLocation());
			}
		} catch (IOException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to save uploaded prospective release file due to " + e.getMessage());
		} catch (JmsException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to send queueing message due to " + e.getMessage());
		} catch (NoSuchAlgorithmException | DecoderException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to write Queued State to Storage Location due to " + e.getMessage());
		} catch (JMSException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to update the RVF state inside the SRS service " + e.getMessage());
		} finally {
			if (responseMap.containsKey(FAILURE_MESSAGE)) {
				try {
					updateRvfStateTo(config, State.FAILED);
				} catch (JsonProcessingException | JMSException e) {
					LOGGER.error(e.getMessage());
				}
			}
		}
	}

	private void updateRvfStateTo(final ValidationRunConfig config, final State state) throws JsonProcessingException, JMSException {
		final String responseQueue = config.getResponseQueue();
		if (responseQueue != null) {
			LOGGER.info("Updating RVF state to queued: {}", responseQueue);
			messagingHelper.send(responseQueue, new ValidationStatusResponse(config, state));
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
