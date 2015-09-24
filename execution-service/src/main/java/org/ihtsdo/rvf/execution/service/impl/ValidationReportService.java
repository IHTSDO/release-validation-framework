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

import javax.annotation.Resource;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
@Service
public class ValidationReportService {

	private FileHelper s3Helper;
	@Resource
	private S3Client s3Client;
	
	private final Logger logger = LoggerFactory.getLogger(ValidationReportService.class);
	@Autowired
	private String bucketName;
	
	private String stateFilePath;
	private String resultsFilePath;
	private String progressFilePath;
	
	private static final String UTF_8 = "UTF-8";
	
	
	public enum State { QUEUED, READY, RUNNING, FAILED, COMPLETE,  } 
	
	
	private ValidationRunConfig validationConfig;
	
	public ValidationReportService(final String bucketName) {
		this.bucketName = bucketName;
	}
	
	public void init(ValidationRunConfig config) {
		validationConfig = config;
		s3Helper = new FileHelper(bucketName, s3Client);
		stateFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "state.txt";
		resultsFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "results.json";
		progressFilePath = config.getStorageLocation() + File.separator + "rvf" + File.separator + "progress.txt";
	}
	
	
	public void writeResults(final Map<String , Object> responseMap, final State state) throws IOException, NoSuchAlgorithmException, DecoderException {
		final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
		final File temp = File.createTempFile("resultJson", ".tmp"); 
		try {
			try (final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),Charset.forName(UTF_8)))) {
				prettyGson.toJson(responseMap, bw);
				//Now copy to our S3 Location
			} 
			s3Helper.putFile(temp, resultsFilePath);
		} finally {
			temp.delete();
		}
		writeState(state);
	}
	
	public void writeState(final State state) throws IOException, NoSuchAlgorithmException, DecoderException {
		logger.info("RVF run {} setting state as {}", validationConfig.getRunId(), state.toString());
		writeToS3(state.name(), stateFilePath);
	}
	
	public void writeProgress(final String progress) {
		try {
			writeToS3(progress, progressFilePath);
		} catch (NoSuchAlgorithmException | IOException | DecoderException e) {
			logger.error("Failed to write progress to S3: " + progressFilePath);
		}
		
	}
	 public void writeToS3(final String writeMe, final String targetPath) throws IOException, NoSuchAlgorithmException, DecoderException {
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
	 
	 public String recoverProgress() {
			final InputStream is = s3Helper.getFileStream(progressFilePath);
			String progressMsg = new String("Failed to read from " + progressFilePath);
			if (is == null) {
				logger.warn("Failed to find progress file {}, in bucket {}", progressFilePath, bucketName);
			} else {
				try {
					progressMsg = IOUtils.toString(is, UTF_8);
				} catch (final IOException e) {
					logger.warn("Failed to read data from progress file {}, in bucket {}", progressFilePath, bucketName);
				}
			}
			return progressMsg;
		}
	 
	 public void recoverResult(final Map<String, Object> responseMap) throws IOException {
			final InputStream is = s3Helper.getFileStream(resultsFilePath);
			Object jsonResults = null;
			if (is == null) {
				logger.warn("Failed to find results file {}, in bucket {}", stateFilePath, bucketName);
			} else {
				final Gson gson = new Gson();
				jsonResults = gson.fromJson(new InputStreamReader(is,Charset.forName(UTF_8)), Map.class);
			}
			if (jsonResults == null) {
				jsonResults = new String("Failed to recover results in " + resultsFilePath);
			}
			responseMap.put("RVF Validation Result", jsonResults);
		}
	 
	 
		public State getCurrentState() {
			State currentState = null;
			try {
				final InputStream is = s3Helper.getFileStream(stateFilePath);
				if (is == null) {
					logger.warn("Failed to find state file {}, in bucket {}", stateFilePath, bucketName);
				}
				final String stateStr = IOUtils.toString(is, UTF_8);
				currentState = State.valueOf(stateStr);
			} catch (final Exception e) {
				logger.warn("Failed to determine validation run state in file {} due to {}", stateFilePath, e.toString());
			}
			return currentState;
		}
	 

}
