package org.ihtsdo.rvf.execution.service;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.ComponentFactory;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.ihtsdo.rvf.entity.TestType;
import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.execution.service.traceability.ChangeSummaryReport;
import org.ihtsdo.rvf.execution.service.traceability.ComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Service
public class TraceabilityComparisonService {

	public static final String ASSERTION_ID_ALL_COMPONENTS_IN_TRACEABILITY_ALSO_IN_DELTA = "f68c761b-3b5c-4223-bc00-6e181e7f68c3";
	public static final String ASSERTION_ID_ALL_COMPONENTS_IN_DELTA_ALSO_IN_TRACEABILITY = "b7f727d7-9226-4eef-9a7e-47a8580f6e7a";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final RestTemplate traceabilityServiceRestTemplate;

	private static final ParameterizedTypeReference<ChangeSummaryReport> CHANGE_SUMMARY_REPORT_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

	// 900000000000534007 | Module dependency reference set (foundation metadata concept) | - Ignore these because they will be generated during the export in Snowstorm.
	private final Set<String> refsetsToIgnore = Sets.newHashSet("900000000000534007");

	public TraceabilityComparisonService(@Value("${traceability-service.url}") String traceabilityServiceUrl) {
		if (!Strings.isNullOrEmpty(traceabilityServiceUrl)) {
			traceabilityServiceRestTemplate = new RestTemplateBuilder().rootUri(traceabilityServiceUrl).build();
		} else {
			traceabilityServiceRestTemplate = null;
		}
	}

	public void runTraceabilityComparison(ValidationStatusReport report, ValidationRunConfig validationConfig) {
		if (traceabilityServiceRestTemplate == null) {
			final String message = "Traceability test requested but service URL not configured.";
			logger.warn(message);
			report.addFailureMessage(message);
			return;
		}
		final String branchPath = validationConfig.getBranchPath();
		if (Strings.isNullOrEmpty(branchPath)) {
			final String message = "Traceability test requested but no branchPath given.";
			logger.warn(message);
			report.addFailureMessage(message);
			return;
		}
		try {
			final Map<String, String> rf2ComponentIdToConceptIdMap = new HashMap<>();
			final Map<ComponentType, Set<String>> rf2Changes = gatherRF2ComponentChanges(validationConfig, rf2ComponentIdToConceptIdMap);

			ChangeSummaryReport summaryReport = getTraceabilityChangeSummaryReport(branchPath);
			Map<ComponentType, Set<String>> traceabilityChanges = Collections.emptyMap();
			Map<String, String> traceabilityComponentIdToConceptIdMap = Collections.emptyMap();
			if (summaryReport != null && summaryReport.getComponentChanges() != null) {
				traceabilityChanges = summaryReport.getComponentChanges();
			}
			if (summaryReport != null && summaryReport.getComponentToConceptIdMap() != null) {
				traceabilityComponentIdToConceptIdMap = summaryReport.getComponentToConceptIdMap();
			}

			final TestRunItem traceabilityAndDelta = new TestRunItem();
			traceabilityAndDelta.setTestType(TestType.TRACEABILITY);
			traceabilityAndDelta.setAssertionUuid(UUID.fromString(ASSERTION_ID_ALL_COMPONENTS_IN_TRACEABILITY_ALSO_IN_DELTA));
			traceabilityAndDelta.setAssertionText("All components in the branch traceability summary must also be in the RF2 export.");
			traceabilityAndDelta.setFailureCount(0L);// Initial value
			AtomicInteger remainingFailureExport = new AtomicInteger(validationConfig.getFailureExportMax());

			diffAndAddFailures(ComponentType.CONCEPT, "Concept change for %s in traceability is missing from RF2 export.",
					traceabilityChanges, rf2Changes, remainingFailureExport, traceabilityAndDelta, traceabilityComponentIdToConceptIdMap);
			diffAndAddFailures(ComponentType.DESCRIPTION, "Description change for %s in traceability is missing from RF2 export.",
					traceabilityChanges, rf2Changes, remainingFailureExport, traceabilityAndDelta, traceabilityComponentIdToConceptIdMap);
			diffAndAddFailures(ComponentType.RELATIONSHIP, "Relationship change for %s in traceability is missing from RF2 export.",
					traceabilityChanges, rf2Changes, remainingFailureExport, traceabilityAndDelta, traceabilityComponentIdToConceptIdMap);
			diffAndAddFailures(ComponentType.REFERENCE_SET_MEMBER, "Refset member change for %s in traceability is missing from RF2 export.",
					traceabilityChanges, rf2Changes, remainingFailureExport, traceabilityAndDelta, traceabilityComponentIdToConceptIdMap);

			if (traceabilityAndDelta.getFirstNInstances().isEmpty()) {
				report.getResultReport().addPassedAssertions(Collections.singletonList(traceabilityAndDelta));
			} else {
				report.getResultReport().addFailedAssertions(Collections.singletonList(traceabilityAndDelta));
			}


			final TestRunItem deltaAndTraceability = new TestRunItem();
			deltaAndTraceability.setTestType(TestType.TRACEABILITY);
			deltaAndTraceability.setAssertionUuid(UUID.fromString(ASSERTION_ID_ALL_COMPONENTS_IN_DELTA_ALSO_IN_TRACEABILITY));
			deltaAndTraceability.setAssertionText("All components in the RF2 export must also be in the branch traceability summary.");
			deltaAndTraceability.setFailureCount(0L);// Initial value
			remainingFailureExport = new AtomicInteger(validationConfig.getFailureExportMax());

			diffAndAddFailures(ComponentType.CONCEPT, "Concept change for %s in RF2 export is missing from traceability.",
					rf2Changes, traceabilityChanges, remainingFailureExport, deltaAndTraceability, rf2ComponentIdToConceptIdMap);
			diffAndAddFailures(ComponentType.DESCRIPTION, "Description change for %s in RF2 export is missing from traceability.",
					rf2Changes, traceabilityChanges, remainingFailureExport, deltaAndTraceability, rf2ComponentIdToConceptIdMap);
			diffAndAddFailures(ComponentType.RELATIONSHIP, "Relationship change for %s in RF2 export is missing from traceability.",
					rf2Changes, traceabilityChanges, remainingFailureExport, deltaAndTraceability, rf2ComponentIdToConceptIdMap);
			diffAndAddFailures(ComponentType.REFERENCE_SET_MEMBER, "Refset member change for %s in RF2 export is missing from traceability.",
					rf2Changes, traceabilityChanges, remainingFailureExport, deltaAndTraceability, rf2ComponentIdToConceptIdMap);

			if (deltaAndTraceability.getFirstNInstances().isEmpty()) {
				report.getResultReport().addPassedAssertions(Collections.singletonList(deltaAndTraceability));
			} else {
				report.getResultReport().addFailedAssertions(Collections.singletonList(deltaAndTraceability));
			}

		} catch (IOException | ReleaseImportException e) {
			String message = "Traceability validation failed";
			logger.error(message, e);
			report.addFailureMessage(e.getMessage() != null ? message + " due to error: " + e.getMessage() : message);
		}
	}

	private void diffAndAddFailures(ComponentType componentType, String message, Map<ComponentType, Set<String>> leftSide, Map<ComponentType, Set<String>> rightSide,
			AtomicInteger remainingFailureExport, TestRunItem report, Map<String, String> componentToConceptIdMap) {

		final Sets.SetView<String> missing = Sets.difference(leftSide.getOrDefault(componentType, Collections.emptySet()), rightSide.getOrDefault(componentType, Collections.emptySet()));
		for (String inLeftNotRight : missing) {
			if (remainingFailureExport.get() > 0) {
				remainingFailureExport.decrementAndGet();
				report.addFirstNInstance(new FailureDetail(componentToConceptIdMap.get(inLeftNotRight), String.format(message, inLeftNotRight))
						.setComponentId(inLeftNotRight));
			}
			report.setFailureCount(report.getFailureCount() + 1);
		}
	}

	private Map<ComponentType, Set<String>> gatherRF2ComponentChanges(ValidationRunConfig validationConfig, Map<String, String> componentIdToConceptIdMap) throws IOException, ReleaseImportException {
		final String releaseEffectiveTime = StringUtils.isNotBlank(validationConfig.getEffectiveTime()) ? validationConfig.getEffectiveTime().replace("-","") : "";

		logger.info("Collecting component ids from snapshot using effectiveTime filter.");

		Set<String> concepts = new HashSet<>();
		Set<String> descriptions = new HashSet<>();
		Set<String> relationships = new HashSet<>();
		Set<String> concreteRelationships = new HashSet<>();
		Set<String> refsetMembers = new HashSet<>();

		try (final FileInputStream releaseZip = new FileInputStream(validationConfig.getLocalProspectiveFile())) {

			final boolean rf2DeltaOnly = validationConfig.isRf2DeltaOnly();

			Predicate<String> effectiveTimePredicate =
					rf2DeltaOnly ?
							effectiveTime -> true :// Let all delta rows through
							effectiveTime -> inDelta(effectiveTime, releaseEffectiveTime);// Only let snapshot rows through if they have the right date

			final ComponentFactory componentFactory = new ComponentFactory() {
				@Override
				public void preprocessingContent() {
					// Not needed
				}

				@Override
				public void loadingComponentsStarting() {
					// Not needed
				}

				@Override
				public void loadingComponentsCompleted() {
					// Not needed
				}

				@Override
				public void newConceptState(String id, String effectiveTime, String active, String moduleId, String s4) {
					if (effectiveTimePredicate.test(effectiveTime)) {
						concepts.add(id);
						componentIdToConceptIdMap.put(id, id);
					}
				}

				@Override
				public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String s4, String s5, String s6, String s7, String s8) {
					if (effectiveTimePredicate.test(effectiveTime)) {
						descriptions.add(id);
						componentIdToConceptIdMap.put(id, s4);
					}
				}

				@Override
				public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String s4, String s5, String s6, String s7, String s8, String s9) {
					if (effectiveTimePredicate.test(effectiveTime)) {
						relationships.add(id);
						componentIdToConceptIdMap.put(id, s4);
					}
				}

				@Override
				public void newConcreteRelationshipState(String id, String effectiveTime, String active, String moduleId, String s4, String s5, String s6, String s7, String s8, String s9) {
					if (effectiveTimePredicate.test(effectiveTime)) {
						concreteRelationships.add(id);
						componentIdToConceptIdMap.put(id, s4);
					}
				}

				@Override
				public void newReferenceSetMemberState(String[] strings, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... strings1) {
					if (effectiveTimePredicate.test(effectiveTime) && !refsetsToIgnore.contains(refsetId)) {
						refsetMembers.add(id);
						componentIdToConceptIdMap.put(id, referencedComponentId);
					}
				}
			};

			if (rf2DeltaOnly) {
				new ReleaseImporter().loadDeltaReleaseFiles(releaseZip, LoadingProfile.complete, componentFactory, false);
			} else {
				new ReleaseImporter().loadSnapshotReleaseFiles(releaseZip, LoadingProfile.complete, componentFactory, false);
			}

		}

		relationships.addAll(concreteRelationships);

		logger.info("Collected component ids: concepts:{}, descriptions:{}, relationships:{}, refset members:{}.",
				concepts.size(), descriptions.size(), relationships.size(), refsetMembers.size());

		final Map<ComponentType, Set<String>> rf2Changes = new EnumMap<>(ComponentType.class);
		rf2Changes.put(ComponentType.CONCEPT, concepts);
		rf2Changes.put(ComponentType.DESCRIPTION, descriptions);
		rf2Changes.put(ComponentType.RELATIONSHIP, relationships);
		rf2Changes.put(ComponentType.REFERENCE_SET_MEMBER, refsetMembers);
		return rf2Changes;
	}

	private ChangeSummaryReport getTraceabilityChangeSummaryReport(String branchPath) {
		logger.info("Fetching diff from traceability service..");
		final String uri = UriComponentsBuilder.fromPath("/change-summary").queryParam("branch", branchPath).toUriString();
		final ResponseEntity<ChangeSummaryReport> response =
				traceabilityServiceRestTemplate.exchange(uri, HttpMethod.GET, null, CHANGE_SUMMARY_REPORT_TYPE_REFERENCE);
		return response.getBody();
	}

	private boolean inDelta(String effectiveTime, String releaseEffectiveTime) {
		return Strings.isNullOrEmpty(releaseEffectiveTime) ? effectiveTime.isEmpty() : releaseEffectiveTime.equals(effectiveTime);
	}

}
