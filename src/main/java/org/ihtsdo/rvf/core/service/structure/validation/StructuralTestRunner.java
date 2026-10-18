package org.ihtsdo.rvf.core.service.structure.validation;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.rvf.core.data.model.FailureDetail;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.data.model.TestType;
import org.ihtsdo.rvf.core.data.model.ValidationReport;
import org.ihtsdo.rvf.core.service.structure.listing.ManifestFile;
import org.ihtsdo.rvf.core.service.structure.resource.ResourceProvider;
import org.ihtsdo.rvf.core.service.structure.resource.ZipFileResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class StructuralTestRunner {
	
	private final Logger logger = LoggerFactory.getLogger(StructuralTestRunner.class);

	private static final String FILE_SIZE_TEST_TYPE = "FileSizeTest";

    private static final String THE_FILE_TXT = "The file ";
	
	protected String reportFolderLocation;
	
	@Value("${rvf.test.report.folder.location}")
	protected File reportDataFolder;
	
	@Value("${rvf.validation.failure.threshold}")
	protected int failureThreshold;
	
	private String structureTestReportPath;

	@Autowired
	private ValidationLogFactory validationLogFactory;
	

	public TestReportable execute(final ResourceProvider prospectiveFileResourceProvider, final ResourceProvider previousFileResourceProvider, final PrintWriter writer, final boolean writeSuccesses,
								  final ManifestFile manifest) {
		// the information for the manifest testing
		long start = System.currentTimeMillis();
		final StreamTestReport testReport = new StreamTestReport(new CsvMetadataResultFormatter(), writer, writeSuccesses);
		final ValidationLog validationLog = validationLogFactory.getValidationLog(ColumnPatternTester.class);
		// run manifest tests
		if ( manifest != null) {
			runManifestTests(prospectiveFileResourceProvider, testReport, manifest, validationLogFactory.getValidationLog(ManifestPatternTester.class));
			testReport.addNewLine();
		}
		runColumnTests(prospectiveFileResourceProvider, testReport, validationLog);
		runLineFeedTests(prospectiveFileResourceProvider, testReport);
		runFileSizeTest(prospectiveFileResourceProvider, previousFileResourceProvider, testReport);
		if ( manifest != null) {
			runRF2FileReleaseTypeTester(testReport, manifest);
		}
		
		testReport.getResult();
		final String summary = testReport.writeSummary();
		validationLog.info(summary);
		logger.debug("Time taken for structure validation: {}", (System.currentTimeMillis() - start));
		return testReport;
	}

    private void runFileSizeTest(ResourceProvider prospectiveFileResourceProvider, ResourceProvider previousFileResourceProvider, StreamTestReport testReport) {
        if (prospectiveFileResourceProvider != null && previousFileResourceProvider != null) {
            runSnapshotFileSizeTest(prospectiveFileResourceProvider, previousFileResourceProvider, testReport);
            runFullFileSizeTest(prospectiveFileResourceProvider, previousFileResourceProvider, testReport);
        }
    }

    /**
     * Compares every snapshot file against the previous release's, by size.
     *
     * <p>Reads the sizes from the ZIP's central directory. It used to extract
     * BOTH releases to disk and call {@link File#length()} on the results: four
     * whole release directories across this method and
     * {@link #runFullFileSizeTest}, single-threaded through snomedboot's
     * {@code ReleaseImporter}, for an answer the zip already carries. On the AU
     * edition that was about 100 seconds of a 224 second validation - and it
     * only happened when a previous release was supplied, which is precisely the
     * nightly case.
     *
     * <p>{@code ZipEntry.getSize()} is the uncompressed size and is exact:
     * verified against a full extraction of that release, 76 entries, zero
     * mismatches and none reporting the unknown -1. Where an entry DOES report
     * -1 - a zip written as a stream, with sizes only in the trailing data
     * descriptor - it is skipped rather than guessed at, because a wrong size
     * here is a wrong finding about a release.
     */
    private void runSnapshotFileSizeTest(ResourceProvider prospectiveFileResourceProvider, ResourceProvider previousFileResourceProvider, StreamTestReport testReport) {
        compareFileSizes(prospectiveFileResourceProvider, previousFileResourceProvider, testReport,
                "Snapshot", "Snapshot files must be equal to or greater in size than previous release");
    }

    private void runFullFileSizeTest(ResourceProvider prospectiveFileResourceProvider, ResourceProvider previousFileResourceProvider, StreamTestReport testReport) {
        compareFileSizes(prospectiveFileResourceProvider, previousFileResourceProvider, testReport,
                "Full", "Full files must be equal to or greater in size than previous release");
    }

    /**
     * @param releaseType {@code Snapshot} or {@code Full}, matching the RF2
     *                    filename component that {@code ReleaseImporter}'s
     *                    ImportType selected on
     */
    void compareFileSizes(ResourceProvider prospectiveFileResourceProvider, ResourceProvider previousFileResourceProvider,
                                  StreamTestReport testReport, String releaseType, String assertionText) {
        String prospectivePath = prospectiveFileResourceProvider.getFilePath();
        String previousPath = previousFileResourceProvider.getFilePath();
        Map<String, Long> prospectiveSizes;
        Map<String, Long> previousSizes;
        try {
            prospectiveSizes = entrySizes(prospectivePath, releaseType);
            previousSizes = entrySizes(previousPath, releaseType);
        } catch (IOException e) {
            logger.error("Failed to read the release archives for the {} file size test", releaseType, e);
            return;
        }
        if (prospectiveSizes.isEmpty() || previousSizes.isEmpty()) {
            logger.warn("No {} files found in one of the versions", releaseType);
            return;
        }

        for (Map.Entry<String, Long> prospective : prospectiveSizes.entrySet()) {
            String prospectiveFilename = prospective.getKey();
            for (Map.Entry<String, Long> previous : previousSizes.entrySet()) {
                String previousFilename = previous.getKey();
                // The original pairing rule, unchanged: match on everything
                // before the last underscore, which drops the date suffix, and
                // stop at the first match.
                if (prospectiveFilename.substring(0, prospectiveFilename.lastIndexOf("_"))
                        .equals(previousFilename.substring(0, previousFilename.lastIndexOf("_")))) {
                    if (prospective.getValue() < previous.getValue()) {
                        testReport.addError("0-0", new Date(), prospectiveFilename, prospectivePath, "File Size", FILE_SIZE_TEST_TYPE, assertionText,
                                THE_FILE_TXT + prospectiveFilename + " (" + (prospective.getValue() / 1024) + " KB) is less than previous release file " + previousFilename + " (" + (previous.getValue() / 1024) + " KB)",
                                THE_FILE_TXT + prospectiveFilename + " must be equal to or greater in size than previous release file " + previousFilename, null);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Uncompressed sizes of one release type's RF2 files, keyed on base filename.
     *
     * <p>Entries are flattened to their base name because that is what the old
     * code compared - {@code ReleaseImporter} extracted into one directory and
     * the tests read {@code File.getName()} - and an entry whose name has no
     * underscore is skipped, since the pairing rule indexes on the last one.
     */
    Map<String, Long> entrySizes(String zipPath, String releaseType) throws IOException {
        Map<String, Long> sizes = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(new File(zipPath))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = new File(entry.getName()).getName();
                if (!name.endsWith(".txt") || !name.contains(releaseType) || !name.contains("_")) {
                    continue;
                }
                if (entry.getSize() < 0) {
                    logger.warn("{} reports no uncompressed size; excluded from the {} file size test",
                            name, releaseType);
                    continue;
                }
                sizes.put(name, entry.getSize());
            }
        }
        return sizes;
    }

	private void runLineFeedTests(ResourceProvider resourceManager, StreamTestReport testReport) {
		
		final RF2FileStructureTester lineFeedPatternTest = new RF2FileStructureTester(validationLogFactory.getValidationLog(RF2FileStructureTester.class), 
				resourceManager, testReport);
		lineFeedPatternTest.runTests();
		
	}

	public TestReportable execute(final ResourceProvider resourceManager, final PrintWriter writer, final boolean writeSuccesses) {
		return execute(resourceManager, null, writer, writeSuccesses, null);
	}

	private void runManifestTests(final ResourceProvider resourceManager, final TestReportable report,
			final ManifestFile manifest, final ValidationLog validationLog) {
		final ManifestPatternTester manifestPatternTester = new ManifestPatternTester(validationLog, resourceManager, manifest, report);
		manifestPatternTester.runTests();
	}

	private void runRF2FileReleaseTypeTester(final TestReportable report, final ManifestFile manifest) {
		final RF2FilesReleaseTypeTester rf2FilesReleaseTypeTester = new RF2FilesReleaseTypeTester(
				validationLogFactory.getValidationLog(RF2FilesReleaseTypeTester.class), manifest, report);
		rf2FilesReleaseTypeTester.runTest();
	}

	private void runColumnTests(final ResourceProvider resourceManager, final TestReportable report, final ValidationLog validationLog) {

		final ColumnPatternTester columnPatternTest = new ColumnPatternTester(validationLog, resourceManager, report);
		columnPatternTest.runTests();
	}
	
	public boolean verifyZipFileStructure(final ValidationReport validationReport, final File prospectiveFile, final File previousReleaseFile, final Long runId, final boolean isRf2DeltaOnly, final File manifestFile,
										  final boolean writeSucceses, final String urlPrefix, String storageLocation, Integer maxFailuresExport ) throws IOException {
		boolean isFailed = false;
		final long timeStart = System.currentTimeMillis();
		if (prospectiveFile != null) {
			logger.debug("Start verifying zip file structure of {} against manifest", prospectiveFile.getAbsolutePath());
		}
		// convert groups which is passed as string to assertion groups
		// set up the response in order to stream directly to the response
		final File structureTestReport = new File(getReportDataFolder(), "structure_validation_"+ runId+".txt");
		structureTestReportPath = structureTestReport.getAbsolutePath();
		try (PrintWriter writer = new PrintWriter(structureTestReport)) {
			final ResourceProvider prospectiveFileResourceProvider = new ZipFileResourceProvider(prospectiveFile);
			final ResourceProvider previousFileResourceProvider = previousReleaseFile != null && !isRf2DeltaOnly ? new ZipFileResourceProvider(previousReleaseFile) : null;

			TestReportable report = execute(prospectiveFileResourceProvider, previousFileResourceProvider, writer, writeSucceses, manifestFile == null ? null : new ManifestFile(manifestFile));
			report.getResult();
			logger.info(report.writeSummary());
			// verify if manifest is valid
			if(report.getNumErrors() > 0) {
				validationReport.setReportUrl(urlPrefix + "/result/structure/" + runId + "?storageLocation=" + storageLocation);
				logger.error("No Errors expected but got " + report.getNumErrors() + " errors");
				logger.info("reportPhysicalUrl : " + structureTestReport.getAbsolutePath());
				// pass file name without extension - we add this back when we retrieve using controller
				logger.info("report.getNumErrors() = " + report.getNumErrors());
				logger.info("report.getNumTestRuns() = " + report.getNumTestRuns());
				final double threshold = report.getNumErrors() / report.getNumTestRuns();
				logger.info("threshold = " + threshold);
				
				// Extract failed tests to report instead showing URL only which link to an physical test result
				extractFailedTestsToReport(validationReport, report, maxFailuresExport);
				
				// bail out only if number of test failures exceeds threshold
				if(threshold > getFailureThreshold()){
					isFailed = true;
				}
			}
		}
		final long timeEnd = System.currentTimeMillis();
		validationReport.addTimeTaken((timeEnd-timeStart)/1000);
		logger.debug("Finished verifying zip file structure of {} against manifest", prospectiveFile.getName());
		return isFailed;
	}

	private void extractFailedTestsToReport(final ValidationReport validationReport, TestReportable report, Integer maxFailuresExport) {
		List<StructuralTestRunItem> structuralTestFailItems = report.getFailedItems();
		Map<String,List<FailureDetail>> structuralTestFailItemMap = new HashMap<>();
		for(StructuralTestRunItem structuralTestRunItem : structuralTestFailItems){
			List<FailureDetail> failDetailList;
			if(structuralTestFailItemMap.containsKey(structuralTestRunItem.getFileName())){
				failDetailList = structuralTestFailItemMap.get(structuralTestRunItem.getFileName());
			} else {
				failDetailList = new ArrayList<>();
			}
			String failMsg = "Structural test " + structuralTestRunItem.getTestType() +
					" for file " + structuralTestRunItem.getFileName() +
					" failed at row-column " + structuralTestRunItem.getExecutionId() +
					" with error: " + structuralTestRunItem.getActualExpectedValue();
			FailureDetail testFailItem  = new FailureDetail("",failMsg);
			failDetailList.add(testFailItem);
			structuralTestFailItemMap.put(structuralTestRunItem.getFileName(), failDetailList);
		}
		
		if(!structuralTestFailItemMap.isEmpty()){
			List<TestRunItem> testRunFailItems = new ArrayList<>();
			int firstNInstance = maxFailuresExport != null ? maxFailuresExport : 10;
			for(String key : structuralTestFailItemMap.keySet()){
				List<FailureDetail> failItems = structuralTestFailItemMap.get(key);
				
				TestRunItem item = new TestRunItem();
				item.setTestCategory(key);
				item.setAssertionUuid(UUID.nameUUIDFromBytes(key.getBytes()));
				item.setTestType(TestType.ARCHIVE_STRUCTURAL);
				item.setAssertionText("RF2 Archive Structural test failed for file " + key);
				item.setFirstNInstances(failItems.stream().limit(firstNInstance).collect(Collectors.toList()));
				item.setFailureCount((long) failItems.size());
				testRunFailItems.add(item);
			}
			validationReport.addFailedAssertions(testRunFailItems);
		}
	}

	/**
	 * The init method that sets up the data folder where all reports are stored. This method must always be called
	 * immediately after instantiating this class outside of Spring context.
	 * //todo move to an async process at some time!
	 */
	@PostConstruct
	public void init() throws Exception {
		logger.info("Sct Data Location passed = " + reportFolderLocation);
		if (reportFolderLocation == null || reportFolderLocation.length() == 0) {
			reportFolderLocation = FileUtils.getTempDirectoryPath() + System.getProperty("file.separator") + "rvf-reports";
		}

		reportDataFolder = new File(reportFolderLocation);
		if(!reportDataFolder.exists()){
			if (reportDataFolder.mkdirs()){
				logger.info("Created report folder at : " + reportFolderLocation);
			} else{
				logger.error("Unable to create data folder at path : " + reportFolderLocation);
				throw new IllegalArgumentException("Bailing out because report folder location can not be set to : " + reportFolderLocation);
			}
		}

		logger.info("Using report folder location as :" + reportDataFolder.getAbsolutePath());
	}

	public void setReportFolderLocation(final String reportFolderLocation) {
		this.reportFolderLocation = reportFolderLocation;
	}

	public File getReportDataFolder() {
		return reportDataFolder;
	}

	public String getStructureTestReportFullPath() {
		return this.structureTestReportPath;
	}
	
	public int getFailureThreshold() {
		return failureThreshold;
	}

	public void setFailureThreshold(final int failureThreshold) {
		this.failureThreshold = failureThreshold;
	}
}
