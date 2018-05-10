package org.ihtsdo.rvf.execution.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
@Service
public class ValidationReportService {

	private static final String RVF = "rvf";
	private FileHelper s3Helper;
	@Resource
	private S3Client s3Client;
	
	private final Logger logger = LoggerFactory.getLogger(ValidationReportService.class);
	
	private String bucketName;
	
	private String stateFilePath;
	private String resultsFilePath;
	private String progressFilePath;
	private String structureTestReportPath;
	
	private static final String UTF_8 = "UTF-8";
	
	
	public enum State { QUEUED, READY, RUNNING, FAILED, COMPLETE,  } 
	
	public ValidationReportService(final String bucketName) {
		this.bucketName = bucketName;
	}
	
	@PostConstruct
	public void init() {
		s3Helper = new FileHelper(bucketName, s3Client);
		String rvfRoot = File.separator + RVF + File.separator;
		stateFilePath = rvfRoot + "state.txt";
		resultsFilePath = rvfRoot + "results.json";
		progressFilePath = rvfRoot + "progress.txt";
		structureTestReportPath = rvfRoot + "structure_validation.txt";
	}
	
	public void writeResults(final Map<String , Object> responseMap, final State state, String storageLocation) throws BusinessServiceException {
		final Gson prettyGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		File temp = null;
		try {
	        temp = File.createTempFile("resultJson", ".tmp"); 
			try (final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),Charset.forName(UTF_8)))) {
				prettyGson.toJson(responseMap, bw);
				//Now copy to our S3 Location
			} 
			s3Helper.putFile(temp, storageLocation + resultsFilePath);
			writeState(state, storageLocation);
		} catch (NoSuchAlgorithmException | IOException | DecoderException e) {
			throw new BusinessServiceException("Failed to write results to file.", e);
		} 		
		finally {
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
			//Now copy to our S3 Location
			s3Helper.putFile(temp, targetPath);
		} finally {
			//And clean up
			temp.delete();
		}
	}
	 
	 public String recoverProgress(String storageLocation) {
			String filePath = storageLocation + progressFilePath;
			final InputStream is = s3Helper.getFileStream( filePath);
			String progressMsg = new String("Failed to read from " + filePath);
			if (is == null) {
				logger.warn("Failed to find progress file {}, in bucket {}", filePath, bucketName);
			} else {
				try {
					progressMsg = IOUtils.toString(is, UTF_8);
				} catch (final IOException e) {
					logger.warn("Failed to read data from progress file {}, in bucket {}", filePath, bucketName);
				}
			}
			return progressMsg;
		}
	 
	 public void recoverResult(final Map<String, Object> responseMap, Long runId, String storageLocation) throws IOException {
			String filePath = storageLocation + resultsFilePath;
			final InputStream is = s3Helper.getFileStream(filePath);
			Object jsonResults = null;
			if (is == null) {
				logger.warn("Failed to find results file {}, in bucket {}", filePath, bucketName);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				jsonResults  = mapper.readValue(new InputStreamReader(is, Charset.forName(UTF_8)), Map.class);
			}
			if (jsonResults == null) {
				jsonResults = new String("Failed to recover results in " + filePath);
			}
			responseMap.put("rvfValidationResult", jsonResults);
		}
	 
	 
		public State getCurrentState(Long runId, String storageLocation) {
			State currentState = null;
			String filePath = storageLocation + stateFilePath;
			try {
				final InputStream is = s3Helper.getFileStream(filePath);
				if (is == null) {
					logger.warn("Failed to find state file {}, in bucket {}", filePath, bucketName);
				}
				final String stateStr = IOUtils.toString(is, UTF_8);
				currentState = State.valueOf(stateStr);
			} catch (final Exception e) {
				logger.warn("Failed to determine validation run state in file {} due to {}", filePath, e.toString());
			}
			return currentState;
		}

	public void putFileIntoS3(String reportStorage, File file) throws NoSuchAlgorithmException, IOException, DecoderException {
			s3Helper.putFile(file, reportStorage + structureTestReportPath);
		}
	
	 public InputStream getStructureReport( Long runId, String storageLocation) throws IOException {
			String filePath = storageLocation + structureTestReportPath;
			return s3Helper.getFileStream(filePath);
		}
	 
}
