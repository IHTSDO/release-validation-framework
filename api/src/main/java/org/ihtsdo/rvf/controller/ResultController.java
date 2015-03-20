package org.ihtsdo.rvf.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunner;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunner.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/result")
public class ResultController {
	
	private static final String MESSAGE = "Message";
	@Autowired
	Provider<ValidationRunner> validationRunnerProvider;

	@RequestMapping(value = "{runId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getResult(@PathVariable final Long runId, 
			@RequestParam(value = "storageLocation") final String storageLocation) throws IOException {
		final ValidationRunner validationRunner = validationRunnerProvider.get();
		final ValidationRunConfig config = new ValidationRunConfig();
		config.addRunId(runId).addStorageLocation(storageLocation);
		validationRunner.setConfig(config);
		//Can we find an rvf status file at that location?  Return 404 if not.
		final Map <String, Object> responseMap = new LinkedHashMap<>();
		final State state = validationRunner.getCurrentState();
		HttpStatus returnStatus = HttpStatus.NON_AUTHORITATIVE_INFORMATION; 
		if (state == null) {
			returnStatus = HttpStatus.NOT_FOUND;
			responseMap.put(ValidationRunner.FAILURE_MESSAGE, "No validation state found at " + storageLocation);
		} else {
			responseMap.put("Status", state.toString());
			switch (state) {
				case READY : 	returnStatus = HttpStatus.LOCKED;
								responseMap.put(MESSAGE, "Validation hasn't started running yet!");
								break;
				case RUNNING :  returnStatus = HttpStatus.LOCKED;
								responseMap.put(MESSAGE, "Validation is still running.");
								break;
				case FAILED :   returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
								validationRunner.recoverResult(responseMap);
								break;
				case COMPLETE : returnStatus = HttpStatus.OK;
								validationRunner.recoverResult(responseMap);
								break;
			}
		}

		return new ResponseEntity<>(responseMap, returnStatus);
	}

}
