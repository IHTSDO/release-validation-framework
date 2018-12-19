package org.ihtsdo.rvf.messaging;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	private FileHelper s3Helper;
	@Resource
	private S3Client s3Client;

	@Value("${executionBucketName}")
	private String s3ExecutionBucketName;
	
	@Value("${publishBucketName}")
	private String s3PublishBucketName;
	
	@Value("${rvf.execution.isAutoScalingEnabled}")
	private Boolean isAutoScalingEnabled;
	
	@Value("${rvf.validation.queue.name}")
	private String destinationName;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ValidationQueueManager.class);

	@PostConstruct
	public void init() {
		s3Helper = new FileHelper(s3ExecutionBucketName, s3Client);
	}

	public void queueValidationRequest(ValidationRunConfig config,
			Map<String, String> responseMap) {
		try {
			config.setS3ExecutionBucketName(s3ExecutionBucketName);
			config.setS3PublishBucketName(s3PublishBucketName);
			if (saveUploadedFiles(config, responseMap)) {
				Gson gson = new Gson();
				String configJson = gson.toJson(config);
				LOGGER.info("Send Jms message to queue for validation config json:"
						+ configJson);
				jmsTemplate.convertAndSend(destinationName, configJson);
				reportService.writeState(State.QUEUED, config.getStorageLocation());
			}
		} catch (IOException e) {
			responseMap.put(FAILURE_MESSAGE,
					"Failed to save uploaded prospective release file due to "
							+ e.getMessage());
		} catch (JmsException e) {
			responseMap.put(FAILURE_MESSAGE,
					"Failed to send queueing message due to " + e.getMessage());
		} catch (NoSuchAlgorithmException | DecoderException e) {
			responseMap.put(FAILURE_MESSAGE,
					"Failed to write Queued State to Storage Location due to "
							+ e.getMessage());
		}
	}

	/*
	 * The issue here is that spring cleans up Multipart files when Dispatcher
	 * is complete, so we need to save off the file before we allow the parent
	 * thread to finish.
	 */
	private boolean saveUploadedFiles(final ValidationRunConfig config,
			final Map<String, String> responseMap) throws IOException {

		if (!config.isProspectiveFilesInS3()) {
			final String filename = config.getFile().getOriginalFilename();
			if (!isAutoScalingEnabled.booleanValue()) {
				LOGGER.info("Autoscaling is not enabled. RVF will tansfer files locally");
				// temp file will be deleted when validation is done.
				final File tempFile = File.createTempFile(filename, ".zip");
				if (!filename.endsWith(".zip")) {
					responseMap.put(FAILURE_MESSAGE,
							"Post condition test package has to be zipped up");
					return false;
				}
				config.getFile().transferTo(tempFile);
				config.setTestFileName(filename);
				config.setProspectiveFileFullPath(tempFile.getAbsolutePath());
				File manifestLocalFile = null;
				if (config.getManifestFile() != null) {
					manifestLocalFile = File.createTempFile(
							config.getManifestFile().getOriginalFilename()
									+ config.getRunId(), ".xml");
					config.getManifestFile().transferTo(manifestLocalFile);
					config.setLocalManifestFile(manifestLocalFile);
					config.setManifestFileFullPath(manifestLocalFile
							.getAbsolutePath());
				}

			} else {
				LOGGER.info("Autoscaling is enabled. RVF needs to save files to S3 for ec2 worker");
				config.setProspectiveFilesInS3(true);
				String s3StoragePath = config.getStorageLocation()
						+ File.separator + FILES_TO_VALIDATE + File.separator;
				String targetFilePath = s3StoragePath + filename;
				s3Helper.putFile(config.getFile().getInputStream(),
						targetFilePath);
				config.setProspectiveFileFullPath(targetFilePath);
				config.setTestFileName(filename);
				if (config.getManifestFile() != null) {
					String manifestS3Path = s3StoragePath
							+ config.getManifestFile().getOriginalFilename();
					s3Helper.putFile(config.getManifestFile().getInputStream(),
							manifestS3Path);
					config.setManifestFileFullPath(manifestS3Path);
				}
			}
		}
		return true;
	}
}
