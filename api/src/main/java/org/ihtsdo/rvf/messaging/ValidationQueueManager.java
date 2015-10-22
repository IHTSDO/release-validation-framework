package org.ihtsdo.rvf.messaging;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

@Service
public class ValidationQueueManager {
	
	private static final String FAILURE_MESSAGE = "failureMessage";
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private ValidationReportService reportService;
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationQueueManager.class);


	public void queueValidationRequest(ValidationRunConfig config, Map<String, String> responseMap) {

		try {
			if (saveUploadedFiles(config, responseMap)) {
				Gson gson = new Gson();
				String configJson = gson.toJson(config);
				logger.info("Send Jms message to queue for validation config json:" + configJson);
				jmsTemplate.convertAndSend(configJson); // Send to default queue
				reportService.writeState(State.QUEUED, config.getStorageLocation());
			}
		} catch (IOException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to save uploaded prospective release file due to " + e.getMessage());
		}
		catch (JmsException e) {

			responseMap.put(FAILURE_MESSAGE, "Failed to send queueing message due to " + e.getMessage());

		} catch (NoSuchAlgorithmException | DecoderException e) {
			responseMap.put(FAILURE_MESSAGE, "Failed to write Queued State to Storage Location due to " + e.getMessage());
		}
	}
	
	
	/*
	 * The issue here is that spring cleans up Multipart files when Dispatcher is complete, so 
	 * we need to save off the file before we allow the parent thread to finish.
	 */
	private boolean saveUploadedFiles(final ValidationRunConfig config, final Map<String, String> responseMap) throws IOException {
		final String filename = config.getFile().getOriginalFilename();
		//temp file will be deleted when validation is done.
		final File tempFile = File.createTempFile(filename, ".zip");
		if (!filename.endsWith(".zip")) {
			responseMap.put(FAILURE_MESSAGE, "Post condition test package has to be zipped up");
			return false;
		}
		// must be a zip, save it off
		config.getFile().transferTo(tempFile);	
		config.setProspectiveFile(tempFile);
		config.setTestFileName(filename);
		if ( config.getManifestFile() != null ) {
			File manifestLocalFile = File.createTempFile( config.getManifestFile().getOriginalFilename() + config.getRunId(), ".xml");
			config.getManifestFile().transferTo(manifestLocalFile);
			config.setManifestFileFullPath(manifestLocalFile.getAbsolutePath());
		}
		return true;
	}
}
