package org.ihtsdo.rvf.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.execution.service.ValidationReportService;
import org.ihtsdo.rvf.execution.service.ValidationReportService.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/result")
@Api(tags = "Validation results", description ="-")
public class ResultController {

	private static final String MESSAGE = "Message";
	@Autowired
	private ValidationReportService reportService;

	@RequestMapping(value = "{runId}", method = RequestMethod.GET)
	@ApiOperation(value = "Retrieve the validation report for a given run id and storage location.", notes = "Retrieves the validation report specified by the runId and storageLocation.")
	public ResponseEntity<Map<String, Object>> getResult(
			@ApiParam(value="Unique number") @PathVariable final Long runId,
			@RequestParam(value = "storageLocation") final String storageLocation)
			throws IOException {
		// Can we find an rvf status file at that location? Return 404 if not.
		final Map<String, Object> responseMap = new LinkedHashMap<>();
		final State state = reportService.getCurrentState(runId,
				storageLocation);
		final HttpStatus returnStatus = HttpStatus.OK;
		if (state == null) {
			responseMap.put(MESSAGE, "No validation state found at " + storageLocation);
		} else {
			responseMap.put("status", state.toString());
			switch (state) {
			case READY:
			case QUEUED:
				responseMap.put(MESSAGE,
						"Validation hasn't started running yet!");
				break;
			case RUNNING:
				 String progress = reportService.recoverProgress(storageLocation);
				responseMap.put(MESSAGE, "Validation is still running.");
				responseMap.put("Progress", progress);
				break;
			case FAILED:
				reportService.recoverResult(responseMap, runId, storageLocation);
				break;
			case COMPLETE:
				reportService.recoverResult(responseMap, runId, storageLocation);
				break;
			}
		}
		return new ResponseEntity<>(responseMap, returnStatus);
	}

	@RequestMapping(value = "/structure/{runId}", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation(value = "Returns a structure test report", notes = "Retrieves the structure test report as txt file for the runId and storage location.")
	@ApiIgnore
	public FileSystemResource getStructureReport(
			@PathVariable final Long runId,
			@RequestParam(value = "storageLocation") final String storageLocation)
			throws IOException {
		File tempReport = File.createTempFile(
				"structure_validation_" + runId.toString(), ".txt");
		try (Writer writer = new FileWriter(tempReport);
				InputStream reportInputStream = reportService
						.getStructureReport(runId, storageLocation)) {
			if (reportInputStream != null) {
				IOUtils.copy(reportInputStream, writer, "UTF-8");
			} else {
				String msg = "No structure report found for runId:" + runId
						+ " at " + storageLocation;
				writer.append(msg);
			}
			return new FileSystemResource(tempReport);
		}
	}
}
