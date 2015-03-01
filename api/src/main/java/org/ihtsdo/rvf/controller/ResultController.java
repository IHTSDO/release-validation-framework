package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.execution.service.impl.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.impl.ValidationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

@Controller
@RequestMapping("/result")
public class ResultController {
	
	@Autowired
	Provider<ValidationRunner> validationRunnerProvider;

	@RequestMapping(value = "{runId}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getResult(@PathVariable Long runId, 
			@RequestParam(value = "storageLocation") String storageLocation) throws IOException {
		ValidationRunner validationRunner = validationRunnerProvider.get();
		ValidationRunConfig config = new ValidationRunConfig();
		config.addRunId(runId).addStorageLocation(storageLocation);
		validationRunner.setConfig(config);
		//Can we find an rvf status file at that location?  Return 404 if not.
		ValidationRunner.State state = validationRunner.getCurrentState();
		Map <String, Object> responseMap = new HashMap<String, Object>();
		responseMap.put("Status", state.toString());
		HttpStatus returnStatus = HttpStatus.NON_AUTHORITATIVE_INFORMATION; 
		if (state == null) {
			returnStatus = HttpStatus.NOT_FOUND;
			responseMap.put(ValidationRunner.FAILURE_MESSAGE, "No validation state found at " + storageLocation);
		} else {
			switch (state) {
				case READY : 	returnStatus = HttpStatus.LOCKED;
								responseMap.put(ValidationRunner.FAILURE_MESSAGE, "Validation hasn't started running yet!");
								break;
				case RUNNING :  returnStatus = HttpStatus.LOCKED;
								responseMap.put(ValidationRunner.FAILURE_MESSAGE, "Validation is still running.");
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
