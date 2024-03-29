package org.ihtsdo.rvf.core.service.structure.validation;

import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * To verify that each line (including last line) in RF2 file is terminated by CR followed by LF (i.e "\r\n").
 *
 */
public class RF2FileStructureTester {
	
	private static final String EMPTY_FILE_CHECKING = " empty file checking";
	private static final String RF2_LINE_SEPARATOR = "\r\n";
	private static final String TEST_TYPE = "line terminator check";
	private final ValidationLog validationLog;
	private final ResourceProvider resourceManager;
	private final TestReportable testReport;
	private Date startTime;
	private static final Logger LOGGER = LoggerFactory.getLogger(RF2FileStructureTester.class);
	
	
	/**
	 * @param validationLog Logger instance
	 * @param resourceManager Resource provider
	 * @param testReport Test report
	 */
	public RF2FileStructureTester(final ValidationLog validationLog, final ResourceProvider resourceManager, StreamTestReport testReport) {
		
		this.validationLog = validationLog;
		this.resourceManager = resourceManager;
		this.testReport = testReport;
	}
	
	public void runTests(){
		startTime = new Date();
		List<String> fileNames = resourceManager.getFileNames();
		ExecutorService executorService = Executors.newCachedThreadPool();
		List<Future<Boolean>> futures = new ArrayList<>();
		for (final String fileName : fileNames) {
			if (!fileName.endsWith(".txt")) {
				continue;
			}
			Future<Boolean> task = executorService.submit(() -> runTestForFile(fileName));
			futures.add(task);
		}
		for (Future<Boolean> task : futures) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error("Task failed when structure testing due to:", e);
				validationLog.executionError("Error", "Failed to check file due to:" + e.fillInStackTrace());
			}
		}
	}
	
	private boolean runTestForFile(String fileName) {
		//check total line numbers match or not
		int totalLine = 0;
		int totalLineScanned = 0;
		try (BufferedReader reader = resourceManager.getReader(fileName, StandardCharsets.UTF_8);
			 Scanner scanner = new Scanner(resourceManager.getReader(fileName, StandardCharsets.UTF_8))) {
			scanner.useDelimiter(RF2_LINE_SEPARATOR);
			while ((reader.readLine()) != null) {
				totalLine++;
			}
			if (totalLine == 0) {
				testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), fileName + EMPTY_FILE_CHECKING, EMPTY_FILE_CHECKING, null,"total line is :" + totalLine, " RF2 file can't be empty and should at least have a header line",null);
			}
			while (scanner.hasNext()) {
				scanner.next();
				totalLineScanned++;
			}
			if (totalLineScanned < totalLine) {
				testReport.addError("0-0", startTime, fileName, resourceManager.getFilePath(), fileName + " line terminator", TEST_TYPE, null, "total line is terminated with CR+LF:" + totalLineScanned , 
						 "total line is terminated with CR+LF:" + totalLine,null);
			} 
		} catch (Exception e) {
			validationLog.executionError("Error", "Failed to read file:" + fileName);
		}
		
		try (BufferedReader lineReader = new BufferedReader(resourceManager.getReader(fileName, StandardCharsets.UTF_8))) {
			for (int i = 1;i <= totalLine; i++) {
				lineReader.readLine();
				if (i == (totalLine - 1)) {
					int read = -1;
					StringBuilder builder = new StringBuilder();
					while ((read = lineReader.read()) != -1) {
						char charRead = (char)read;
						if ((charRead == '\r') || (charRead == '\n')) {
							builder.append(charRead);
						}
					}
					if (!RF2_LINE_SEPARATOR.contentEquals(builder)) {
						StringBuilder actualResult = new StringBuilder();
						String actualLineSeparator = builder.toString().replace("\n", "LF").replace("\r", "CR");
						actualResult.append("the last line is terminated with[");
						actualResult.append(actualLineSeparator);
						actualResult.append("]");
						testReport.addError(totalLine + "-0", startTime, fileName, resourceManager.getFilePath(), fileName + " ast line terminator",TEST_TYPE, null,
								actualResult.toString(), "the last line is terminated with CR+LF",null);
					}
					break;
				}
			}
		} catch (Exception e) {
			validationLog.executionError("Error", "Failed to read file:" + fileName);
		}
		return true;
	}
}
