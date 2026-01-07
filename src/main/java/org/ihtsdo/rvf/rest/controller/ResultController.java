package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.core.service.ValidationReportService;
import org.ihtsdo.rvf.core.service.ValidationReportService.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/result")
@Tag(name = "Validation Results")
public class ResultController {

	private static final String MESSAGE = "Message";
	@Autowired
	private ValidationReportService reportService;

	@RequestMapping(value = "{runId}", method = RequestMethod.GET)
	@Operation(summary = "Retrieve the validation report for a given run id and storage location.", description = "Retrieves the validation report specified by the runId and storageLocation.")
	public ResponseEntity<Map<String, Object>> getResult(
			@Parameter(description = "Unique number") @PathVariable final Long runId,
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
                case READY, QUEUED -> responseMap.put(MESSAGE, "Validation hasn't started running yet!");
                case RUNNING -> {
                    String progress = reportService.recoverProgress(storageLocation);
                    responseMap.put(MESSAGE, "Validation is still running.");
                    responseMap.put("Progress", progress);
                }
                default -> reportService.recoverResult(responseMap, storageLocation);
            }
		}
		return new ResponseEntity<>(responseMap, returnStatus);
	}

	@RequestMapping(value = "/structure/{runId}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(hidden = true, summary = "Returns a structure test report", description = "Retrieves the structure test report as txt file for the runId and storage location.")
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
