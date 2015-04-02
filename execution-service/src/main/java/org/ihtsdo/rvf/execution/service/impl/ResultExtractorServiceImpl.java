package org.ihtsdo.rvf.execution.service.impl;

import java.util.List;
import java.util.UUID;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.execution.service.ResultExtractorService;
import org.ihtsdo.rvf.service.AssertionService;
import org.ihtsdo.rvf.service.QAResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
@Service
public class ResultExtractorServiceImpl implements ResultExtractorService {

	@Autowired QAResultService qaResultService;
	@Autowired AssertionService assertionService;
	@Override
	public String extractResultToJson(final Long runId, final String assertionUUID) throws BusinessServiceException {
		final Assertion assertion = assertionService.getAssertionByUUID( assertionUUID);
		if ( assertion == null ) {
			throw new BusinessServiceException("No assertion found for assertion UUID:" + assertionUUID);
		}
		final List<String> failures = qaResultService.getResult(runId, assertionUUID);
		if (failures == null || failures.isEmpty()) {
			return  String.format("No results found for runId [%s] and assertion UUID [%s].",runId, assertionUUID);
		}
		final TestRunItem item = new TestRunItem();
		item.setExecutionId(runId.toString());
		item.setAssertionText(assertion.getName());
		item.setAssertionUuid(UUID.fromString(assertionUUID));
		item.setTestCategory(assertion.getKeywords());
		item.setFirstNInstances(failures);
		item.setFailureCount((long) failures.size());
		final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
		return prettyGson.toJson(item);
	}
}
