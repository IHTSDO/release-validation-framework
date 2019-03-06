package org.ihtsdo.rvf.execution.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.config.ValidationJobResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
@Service
public class ValidationReportService {
	
	private static final String RVF = "rvf";
	
	private final Logger logger = LoggerFactory.getLogger(ValidationReportService.class);
	
	@Autowired
	private ValidationJobResourceConfig jobResourceConfig;
	
	@Autowired
	private ResourceLoader cloudResourceLoader;
	
	private String stateFilePath;
	private String resultsFilePath;
	private String progressFilePath;
	private String structureTestReportPath;
	private Gson prettyGson;
	private ResourceManager resourceManager;
	
	private static final String UTF_8 = "UTF-8";
	
	public enum State { QUEUED, READY, RUNNING, FAILED, COMPLETE,  } 
	
	@PostConstruct
	public void init() {
		String rvfRoot = File.separator + RVF + File.separator;
		stateFilePath = rvfRoot + "state.txt";
		resultsFilePath = rvfRoot + "results.json";
		progressFilePath = rvfRoot + "progress.txt";
		structureTestReportPath = rvfRoot + "structure_validation.txt";
		prettyGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		resourceManager = new ResourceManager(jobResourceConfig, cloudResourceLoader);
	}
	
	public void writeResults(ValidationStatusReport statusReport, State state, String storageLocation) throws BusinessServiceException {
		File temp = null;
		try {
			temp = File.createTempFile("resultJson", ".tmp");
			try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), Charset.forName(UTF_8)))) {
				prettyGson.toJson(statusReport, bw);
				//Now copy to our S3 Location
			} 
			resourceManager.writeResource(storageLocation + resultsFilePath, new FileInputStream(temp));
			writeState(state, storageLocation);
		} catch (NoSuchAlgorithmException | IOException | DecoderException e) {
			throw new BusinessServiceException("Failed to write results to file.", e);
		} finally {
			if (temp != null) {
				temp.delete();
			}
		}
	}
	
	public void writeState(final State state, String storageLocation) throws IOException, NoSuchAlgorithmException, DecoderException {
		writeToS3(state.name(), storageLocation + stateFilePath);
	}
	
	public void writeProgress(final String progress,  String storageLocation) {
		String filePath = storageLocation + progressFilePath;
		try {
			writeToS3(progress, filePath);
		} catch (NoSuchAlgorithmException | IOException | DecoderException e) {
			logger.error("Failed to write progress to S3: " + filePath);
		}
		
	}
	
	 private void writeToS3(final String writeMe, final String targetPath) throws IOException, NoSuchAlgorithmException, DecoderException {
		//First write the data to a local temp file
		final File temp = File.createTempFile("tempfile", ".tmp"); 
		try {
			try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),Charset.forName(UTF_8)))) {
				bw.write(writeMe);
			}
			resourceManager.writeResource(targetPath, new FileInputStream(temp));
		} finally {
			//And clean up
			temp.delete();
		}
	}
	 
	 public String recoverProgress(String storageLocation) {
		 String filePath = storageLocation + progressFilePath;
		 String progressMsg = new String("Failed to read from " + filePath);
		 try {
			 try ( InputStream is = resourceManager.readResourceStreamOrNullIfNotExists(filePath)) {
				if (is != null) {
					 progressMsg = IOUtils.toString(is, UTF_8);
				 } else {
					logger.warn("Failed to find progress file {}, via resource config {}", filePath, jobResourceConfig);
				 } 
			 }
		 } catch (IOException e) {
			 logger.error("Failed to read data from progress file {}, via resource config {", filePath, jobResourceConfig);
		 }
		 return progressMsg;
	 }
	 
	 public void recoverResult(final Map<String, Object> responseMap, Long runId, String storageLocation) throws IOException {
			String filePath = storageLocation + resultsFilePath;
			try (InputStream is = resourceManager.readResourceStreamOrNullIfNotExists(filePath);
				InputStreamReader inputStreamReader = new InputStreamReader(is, Charset.forName(UTF_8))) {
				Object jsonResults = null;
				if (is == null) {
					logger.warn("Failed to find results file {}, via resource config {}", filePath, jobResourceConfig);
				} else {
					ObjectMapper mapper = new ObjectMapper();
					jsonResults  = mapper.readValue(inputStreamReader, Map.class);
				}
				if (jsonResults == null) {
					jsonResults = new String("Failed to recover results in " + filePath);
				}
				responseMap.put("rvfValidationResult", jsonResults);
			}
	}
	 
	 
	 public State getCurrentState(Long runId, String storageLocation) {
		 State currentState = null;
		 String filePath = storageLocation + stateFilePath;
		 try {
			 try (InputStream is = resourceManager.readResourceStreamOrNullIfNotExists(filePath)) {
				 if (is == null) {
					 logger.warn("Failed to find state file {}, via resource config {}", filePath, jobResourceConfig);
				 }
				 String stateStr = IOUtils.toString(is, UTF_8);
				 currentState = State.valueOf(stateStr);
			 }
			 
		 } catch (final Exception e) {
			 logger.warn("Failed to determine validation run state in file {} due to {}", filePath, e.toString());
		 }
		 return currentState;
	}

	public void putFileIntoS3(String reportStorage, File file) throws NoSuchAlgorithmException, IOException, DecoderException {
		resourceManager.writeResource(reportStorage + structureTestReportPath, new FileInputStream(file));
	}
	
	public InputStream getStructureReport( Long runId, String storageLocation) throws IOException {
			return resourceManager.readResourceStreamOrNullIfNotExists(storageLocation + structureTestReportPath);
	}
}
