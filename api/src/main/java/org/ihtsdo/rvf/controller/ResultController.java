package org.ihtsdo.rvf.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService;
import org.ihtsdo.rvf.execution.service.impl.ValidationReportService.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@RequestMapping("/result")
public class ResultController {
	
	private static final String MESSAGE = "Message";
	@Autowired
	private ValidationReportService reportService;

	@RequestMapping(value = "{runId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String,Object>> getResult(@PathVariable final Long runId, 
			@RequestParam(value = "storageLocation") final String storageLocation) throws IOException {
		//Can we find an rvf status file at that location?  Return 404 if not.
		final Map<String, Object> responseMap = new LinkedHashMap<>();
		final State state = reportService.getCurrentState( runId, storageLocation);
		final HttpStatus returnStatus = HttpStatus.OK;
		if (state == null) {
			responseMap.put(MESSAGE, "No validation state found at " + storageLocation);
		} else {
			responseMap.put("status", state.toString());
			switch (state) {
				case QUEUED : 	responseMap.put(MESSAGE, "Validation hasn't started running yet!");
								break;
				case RUNNING :  final String progress = reportService.recoverProgress(storageLocation);
								responseMap.put(MESSAGE, "Validation is still running.");
								responseMap.put("Progress", progress);
								break;
				case FAILED :   reportService.recoverResult(responseMap, runId, storageLocation);
								break;
				case COMPLETE : reportService.recoverResult(responseMap, runId, storageLocation);
								break;
			}
		}
		return new ResponseEntity<>(responseMap, returnStatus);
	}
	
	@RequestMapping(value = "/structure/{runId}", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Returns a report",
		notes = "Returns a report as txt file for a valid report id " )
	public FileSystemResource getStructureReport(@PathVariable final Long runId, 
			@RequestParam(value = "storageLocation") final String storageLocation) throws IOException {
		InputStream reportInputStream = reportService.getStructureReport(runId, storageLocation);
		File tempReport = File.createTempFile("structure_validation_"+ runId.toString(), ".txt");
		OutputStream outputSteam = new FileOutputStream(tempReport);
		if ( reportInputStream != null ) {
			IOUtils.copy(reportInputStream, outputSteam);
		} 
		return new FileSystemResource(tempReport);
	}
}
