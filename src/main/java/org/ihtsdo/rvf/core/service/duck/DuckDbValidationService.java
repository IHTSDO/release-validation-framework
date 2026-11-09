package org.ihtsdo.rvf.core.service.duck;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.utils.ExceptionUtils;
import org.ihtsdo.rvf.config.ConditionalOnDuckDbEngine;
import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.data.model.SeverityLevel;
import org.ihtsdo.rvf.core.data.model.TestRunItem;
import org.ihtsdo.rvf.core.data.model.TestType;
import org.ihtsdo.rvf.core.data.model.ValidationReport;
import org.ihtsdo.rvf.core.service.ReleaseAcquisitionService;
import org.ihtsdo.rvf.core.service.SqlAssertionValidationService;
import org.ihtsdo.rvf.core.service.ValidationReportService;
import org.ihtsdo.rvf.core.service.WhitelistService;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.ihtsdo.rvf.core.service.pojo.ValidationStatusReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The SQL-assertion pass on DuckDB: the counterpart of {@code MysqlValidationService}.
 *
 * <p>The sequence is that service's: load the releases, create qa_result, prepare the
 * schema, select the assertions the requested groups name, execute them, read qa_result
 * back, and fill in the report. The differences are the engine's:
 * <ul>
 * <li>Releases are unpacked from the files {@link ReleaseAcquisitionService} acquired and
 *     materialised into a DuckDB database file created for this run and deleted after it,
 *     so there are no schemas to drop between runs and no shared qa_result to truncate.
 * <li>The assertions come from {@link DuckAssertionService}'s corpus, captured once at the
 *     start of the run, so a reload during a run does not change what it executes.
 * <li>A statement that needs a previous or dependency release the run does not have is
 *     skipped rather than run against an empty schema; see {@link DuckBinder#bind}. A
 *     first-time release has an empty previous release, as on MySQL.
 * <li>The releases are loaded before any assertion SQL runs; the connection is then confined
 *     to the run's own directory, {@code rvf-<runId>} in the work directory; see
 *     {@link DuckConnections#lockDown}.
 * </ul>
 */
@Service
@ConditionalOnDuckDbEngine
public class DuckDbValidationService implements SqlAssertionValidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckDbValidationService.class);

	/**
	 * The schema names {@link DuckBinder} binds {@code <PROSPECTIVE>}, {@code <PREVIOUS>} and
	 * {@code <DEPENDENCY>} to. Fixed, not per run: each run has its own database file.
	 */
	private static final String PROSPECTIVE_SCHEMA = "prospective";
	private static final String PREVIOUS_SCHEMA = "previous";
	private static final String DEPENDENCY_SCHEMA = "dependency";

	/**
	 * Every declared table with no rows: the previous release of a first-time release, and
	 * what an anti-join against the dependency reads when there is none; see {@link DuckBinder#bind}.
	 */
	private static final String EMPTY_SCHEMA = "rvf_empty";

	/**
	 * Where an extension is combined with its dependency. A schema of its own, where MySQL
	 * merges into the prospective schema, so the release-type phase's inputs stay intact.
	 */
	private static final String COMBINED_SCHEMA = "combined";

	/** Marks which side of the extension/dependency merge a row came from. */
	private static final String MERGE_SOURCE = "rvf_merge_source";

	private static final String EFFECTIVE_TIME = "effectivetime";

	/** Assertions that compare a package with its own previous release. */
	private static final String RELEASE_TYPE_VALIDATION = "release-type-validation";

	private static final String QA_RESULT_SCHEMA = "rvf_results";

	/** qa_result as RVF creates it, including skip_module_check. */
	static final String QA_RESULT_COLUMNS = "id BIGINT, run_id BIGINT, assertion_id VARCHAR, "
			+ "concept_id BIGINT, details VARCHAR, component_id VARCHAR, table_name VARCHAR, "
			+ "skip_module_check BOOLEAN";

	/** Same text MysqlValidationService writes, so the progress file reads alike. */
	private static final String START_EXECUTING_ASSERTIONS = "Start executing assertions...";

	/** Assertions per progress message, as on the MySQL path. */
	private static final int PROGRESS_EVERY = 10;

	/** Assertions that build shared tables rather than validating anything. */
	static final String RESOURCE_KEYWORD = "resource";

	private static final String SQL_SUMMARY_KEY = TestType.SQL.name();

	/** The effectiveTime suffix of an RF2 file name. */
	private static final Pattern RF2_VERSION = Pattern.compile(".*_(\\d+)\\.txt");

	private final ValidationReportService reportService;
	private final WhitelistService whitelistService;
	private final ReleaseAcquisitionService acquisitionService;
	private final DuckAssertionService assertionService;
	private final String workDirectory;
	private final String emptyReleaseFile;
	private final String qaResultTable;
	private final DuckConnections.Limits limits;
	private final int whitelistBatchSize;
	private final int fsnLookupBatchSize;
	private final DuckReleaseCache releaseCache;

	@Autowired
	public DuckDbValidationService(ValidationReportService reportService,
			WhitelistService whitelistService,
			ReleaseAcquisitionService acquisitionService,
			DuckAssertionService assertionService,
			@Value("${rvf.duck.work.directory:${java.io.tmpdir}}") String workDirectory,
			@Value("${rvf.empty-release-file}") String emptyReleaseFile,
			@Value("${rvf.qa.result.table.name:qa_result}") String qaResultTableName,
			@Value("${rvf.duck.threads:0}") int duckThreads,
			@Value("${rvf.duck.memory.limit:}") String duckMemoryLimit,
			@Value("${rvf.duck.cache.directory:}") String cacheDirectory,
			@Value("${rvf.duck.cache.max-gb:0}") double cacheMaxGb,
			@Value("${rvf.assertion.whitelist.batchsize:1000}") int whitelistBatchSize,
			@Value("${rvf.validation.fsn-lookup-batch-size:1000}") int fsnLookupBatchSize) {
		this.reportService = reportService;
		this.whitelistService = whitelistService;
		this.acquisitionService = acquisitionService;
		this.assertionService = assertionService;
		this.workDirectory = workDirectory;
		this.emptyReleaseFile = emptyReleaseFile;
		this.qaResultTable = QA_RESULT_SCHEMA + "." + qaResultTableName;
		this.limits = new DuckConnections.Limits(duckThreads, duckMemoryLimit);
		this.whitelistBatchSize = whitelistBatchSize;
		this.fsnLookupBatchSize = fsnLookupBatchSize;
		Path cache = Path.of(cacheDirectory == null || cacheDirectory.isBlank()
				? workDirectory + "/release-cache" : cacheDirectory);
		this.releaseCache = new DuckReleaseCache(cache, (long) (cacheMaxGb * 1073741824L), limits);
		if (this.releaseCache.isEnabled()) {
			LOGGER.info("Release cache enabled: {} with a {} GB budget", cache, cacheMaxGb);
		}
		// At startup, so a native library that will not load stops the application rather
		// than failing every validation.
		DuckConnections.requireNativeLibrary();
	}

	/**
	 * The unpacked release directories a run validates.
	 *
	 * @param prospective    the unpacked prospective package
	 * @param shipped        which release types the prospective package ships, or null to read
	 *                       them from {@code prospective}
	 * @param previous       the unpacked previous release, or null when the run has none
	 * @param previousEmpty  the run declared that there is no previous release (a first-time
	 *                       release, or the previous release named {@code rvf.empty-release-file}),
	 *                       so {@code <PREVIOUS>} binds to an empty schema rather than being absent
	 * @param dependencies   the unpacked dependency releases, in the order the run names them;
	 *                       their union is what an extension sits on
	 */
	public record ReleaseDirectories(Path prospective, Shipped shipped, Path previous, boolean previousEmpty,
			List<Dependency> dependencies) {

		public ReleaseDirectories {
			dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
		}

		/** A run with at most one dependency, whose release types are read from the directory. */
		public ReleaseDirectories(Path prospective, Path previous, Path dependency) {
			this(prospective, null, previous, false, dependency == null ? List.of()
					: List.of(new Dependency(dependency.getFileName().toString(), dependency)));
		}

		public static ReleaseDirectories of(Path prospective) {
			return new ReleaseDirectories(prospective, null, null);
		}
	}

	/** One unpacked dependency release, by the release file name the run names it with. */
	public record Dependency(String release, Path directory) {
	}

	/**
	 * Whether a package ships Delta and Full files, decided as
	 * {@code ValidationVersionLoader.rf2DeltaFileExists} and {@code rf2FullFileExists} decide
	 * it: by the presence of a Concept file of that type.
	 */
	public record Shipped(boolean delta, boolean full) {

		private static final Pattern CONCEPT_DELTA = Pattern.compile("x?(sct|rel)2_Concept_[^_]*Delta_.*.txt");
		private static final Pattern CONCEPT_FULL = Pattern.compile("x?(sct|rel)2_Concept_[^_]*Full_.*.txt");

		/** From the package as acquired, before any file is excluded, as MySQL reads it. */
		static Shipped in(File zip) throws IOException {
			try (ZipFile file = new ZipFile(zip)) {
				return of(file.stream().filter(e -> !e.isDirectory()).map(ZipEntry::getName)
						.map(name -> name.substring(name.lastIndexOf('/') + 1)).toList());
			}
		}

		static Shipped in(Path directory) throws IOException {
			try (Stream<Path> walk = Files.walk(directory)) {
				return of(walk.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).toList());
			}
		}

		private static Shipped of(List<String> fileNames) {
			return new Shipped(fileNames.stream().anyMatch(n -> CONCEPT_DELTA.matcher(n).matches()),
					fileNames.stream().anyMatch(n -> CONCEPT_FULL.matcher(n).matches()));
		}
	}

	/**
	 * Unpacks the release files acquisition left on disk and validates them.
	 *
	 * <p>Acquisition hands over one list of zips, {@code getLocalReleaseFiles()}; they are told
	 * apart by name against {@code getPreviousRelease()} and {@code getExtensionDependencies()},
	 * not by position, which depends on which downloads the run made.
	 */
	@Override
	public ValidationStatusReport runRF2Validations(ValidationRunConfig validationConfig,
			ValidationStatusReport statusReport) {
		MysqlExecutionConfig executionConfig = acquisitionService.createExecutionConfig(validationConfig);
		Path work = Path.of(workDirectory, "rvf-" + validationConfig.getRunId());
		try {
			ReleaseDirectories releases = unpack(validationConfig, executionConfig, work);
			return runValidations(executionConfig, releases, validationConfig.getStorageLocation(), statusReport);
		} catch (IOException e) {
			String message = ExceptionUtils.getExceptionCause("Failed to unpack the release files for validation", e);
			LOGGER.error(message, e);
			statusReport.addFailureMessage(message);
			statusReport.getReportSummary().put(SQL_SUMMARY_KEY, message);
			return statusReport;
		} finally {
			FileUtils.deleteQuietly(work.toFile());
		}
	}

	private ReleaseDirectories unpack(ValidationRunConfig validationConfig,
			MysqlExecutionConfig executionConfig, Path work) throws IOException {
		Files.createDirectories(work);
		Collection<String> excluded = executionConfig.getExcludedRF2Files();

		File prospectiveZip = validationConfig.getLocalProspectiveFile();
		if (prospectiveZip == null) {
			throw new IOException("No prospective release file was acquired for this run");
		}
		Path prospective = DuckReleaseUnpacker.unpack(prospectiveZip, work, PROSPECTIVE_SCHEMA, excluded);

		Path previous = null;
		String previousRelease = validationConfig.getPreviousRelease();
		File previousZip = localFileNamed(validationConfig, previousRelease);
		if (previousZip != null) {
			previous = DuckReleaseUnpacker.unpack(previousZip, work, PREVIOUS_SCHEMA, excluded);
		}
		boolean previousEmpty = previous == null
				&& (validationConfig.isFirstTimeRelease() || emptyReleaseFile.equals(previousRelease));

		// Each dependency in a directory of its own, and a schema of its own: the extension
		// sits on their union, and an edition's delta is derived from each one separately.
		List<Dependency> dependencies = new ArrayList<>();
		List<String> names = validationConfig.getExtensionDependencies();
		if (!CollectionUtils.isEmpty(names)) {
			Path dependencyRoot = work.resolve(DEPENDENCY_SCHEMA);
			for (String name : names) {
				File zip = localFileNamed(validationConfig, name);
				if (zip == null) {
					throw new IOException("The dependency release " + name + " was not acquired for this run");
				}
				dependencies.add(new Dependency(name, DuckReleaseUnpacker.unpack(zip, dependencyRoot,
						String.valueOf(dependencies.size()), excluded)));
			}
		}
		return new ReleaseDirectories(prospective, Shipped.in(prospectiveZip), previous, previousEmpty, dependencies);
	}

	private static File localFileNamed(ValidationRunConfig validationConfig, String name) {
		if (!StringUtils.hasLength(name) || validationConfig.getLocalReleaseFiles() == null) {
			return null;
		}
		for (File file : validationConfig.getLocalReleaseFiles()) {
			if (name.equals(file.getName())) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Runs one validation of unpacked releases and returns {@code statusReport}.
	 *
	 * <p>Never throws: every failure becomes a message on the status report, as on the MySQL
	 * path. It does not report results alongside a failure that makes them meaningless.
	 */
	public ValidationStatusReport runValidations(MysqlExecutionConfig executionConfig,
			ReleaseDirectories releases, String reportStorage, ValidationStatusReport statusReport) {
		long timeStart = System.currentTimeMillis();
		if (statusReport.getResultReport() == null) {
			ValidationReport report = new ValidationReport();
			report.setExecutionId(executionConfig.getExecutionId());
			statusReport.setResultReport(report);
		}
		// The run's own directory, which also holds what runRF2Validations unpacked: the one
		// directory its assertions may read or write.
		Path runDirectory = Path.of(workDirectory, "rvf-" + executionConfig.getExecutionId());
		boolean ownsRunDirectory = !Files.isDirectory(runDirectory);
		Path database = runDirectory.resolve("rvf_duck.duckdb");
		try {
			Files.createDirectories(runDirectory);
			// A file database: DuckDB spills to disk beside it, and a full release does not fit
			// in memory. Deleted first, so a crashed attempt at the same run is not reopened.
			deleteDatabase(database);
			try (Connection connection = DriverManager.getConnection("jdbc:duckdb:" + database)) {
				configure(connection);
				return run(connection, executionConfig, releases, runDirectory, reportStorage, statusReport, timeStart);
			}
		} catch (Exception e) {
			String message = ExceptionUtils.getExceptionCause("Failed to run DuckDB validation", e);
			LOGGER.error(message, e);
			statusReport.addFailureMessage(message);
			statusReport.getReportSummary().put(SQL_SUMMARY_KEY, message);
			return statusReport;
		} finally {
			deleteDatabase(database);
			if (ownsRunDirectory) {
				FileUtils.deleteQuietly(runDirectory.toFile());
			}
		}
	}

	/**
	 * Resource limits and MySQL's free conversion between types, which the corpus relies on.
	 * Applied before loading; the connection is locked down once the releases are in.
	 */
	private void configure(Connection connection) throws SQLException {
		limits.apply(connection);
		try (Statement st = connection.createStatement()) {
			st.execute("SET old_implicit_casting=true");
		}
	}

	/**
	 * Loads the releases, then runs the assertions.
	 *
	 * <p>Loading reads the release directories and the release cache, and runs only the
	 * engine's own SQL: table shapes from the store, which {@link DuckStore#parse} has checked
	 * are plain declarations. Cached releases are attached read-only. Only then is the
	 * connection locked down to {@code runDirectory}, its database and spill included, before
	 * any of the store's statements run: they cannot read other runs' files or the release
	 * cache, nor write anywhere else.
	 */
	private ValidationStatusReport run(Connection connection, MysqlExecutionConfig executionConfig,
			ReleaseDirectories releases, Path runDirectory, String reportStorage, ValidationStatusReport statusReport,
			long timeStart) throws IOException {
		// Once per run: a reload during the run must not change what it executes.
		DuckAssertionService.Corpus corpus = assertionService.corpusFor(executionConfig.getAssertionPacks());
		DuckStore store = corpus.store();
		LOGGER.info("DuckDB assertion store: {}{}", store.generatorDescription(),
				corpus.packs().isEmpty() ? "" : " with packs "
						+ corpus.packs().stream().map(DuckStorePacks.Pack::label).toList());

		String lastItemLoadAttempted = "Item Unknown";
		try {
			// In MySQL's order: the previous and dependency releases before the prospective
			// release's missing delta and full tables, which may be derived from them.
			lastItemLoadAttempted = "Prospective Release - " + releases.prospective();
			materialise(connection, releases.prospective(), PROSPECTIVE_SCHEMA, store, false);
			session(connection, PROSPECTIVE_SCHEMA);
			statusReport.setRF2Files(rf2FileNames(releases.prospective()));

			if (releases.previous() != null) {
				lastItemLoadAttempted = "Previous Release - " + releases.previous();
				materialise(connection, releases.previous(), PREVIOUS_SCHEMA, store, true);
			}
			Map<String, String> dependencySchemas = new LinkedHashMap<>();
			for (Dependency dependency : releases.dependencies()) {
				lastItemLoadAttempted = "Dependency Release - " + dependency.release();
				String schema = releases.dependencies().size() == 1
						? DEPENDENCY_SCHEMA : DEPENDENCY_SCHEMA + "_" + dependencySchemas.size();
				materialise(connection, dependency.directory(), schema, store, true);
				dependencySchemas.put(dependency.release(), schema);
			}
			if (dependencySchemas.size() > 1) {
				lastItemLoadAttempted = "Union of the dependency releases";
				unionDependencies(connection, store, dependencySchemas.values());
			}
			lastItemLoadAttempted = "Empty schema";
			Path none = Files.createTempDirectory(runDirectory, "rvf-empty-");
			try {
				materialise(connection, none, EMPTY_SCHEMA, store, false);
			} finally {
				Files.deleteIfExists(none);
			}

			if (executionConfig.isRf2DeltaOnly()) {
				lastItemLoadAttempted = "Delta-only rebuild of the prospective snapshot";
				rebuildSnapshotFromPreviousAndDelta(connection, store, releases);
			} else {
				lastItemLoadAttempted = "Delta and full tables the prospective package does not ship";
				Shipped shipped = releases.shipped() != null ? releases.shipped() : Shipped.in(releases.prospective());
				if (!shipped.delta()) {
					insertIntoProspectiveDeltaTables(connection, store, executionConfig, dependencySchemas);
				}
				if (!shipped.full()) {
					insertIntoProspectiveFullTables(connection, store);
				}
			}
			createResultTable(connection);
			lastItemLoadAttempted = "Confining the connection to " + runDirectory;
			DuckConnections.lockDown(connection, List.of(runDirectory));
		} catch (Exception e) {
			String message = ExceptionUtils.getExceptionCause(
					String.format("Failed to load data (%s) into DuckDB", lastItemLoadAttempted), e);
			LOGGER.error(message, e);
			statusReport.addFailureMessage(message);
			statusReport.getReportSummary().put(SQL_SUMMARY_KEY, message);
			return statusReport;
		}

		DuckAssertionSource source = corpus.source();
		Selection selection = selectAssertions(source, executionConfig);
		LOGGER.info("Total assertions to run {} for groups {}", selection.total(), executionConfig.getGroupNames());

		// One <VERSION> for both phases of an extension run.
		String version = releaseVersion(releases.prospective());
		String previousSchema = previousSchema(executionConfig, releases);

		List<TestRunItem> items;
		try {
			items = executionConfig.isExtensionValidation() && !executionConfig.isReleaseAsAnEdition()
					? runExtensionSplit(connection, store, executionConfig, releases, previousSchema, reportStorage,
							selection, version)
					: runSinglePhase(connection, store, executionConfig, releases, previousSchema, reportStorage,
							selection, version);
		} catch (DuckDbAssertionExecutionService.SetupFailedException e) {
			LOGGER.error("DuckDB setup failed - abandoning run {}", executionConfig.getExecutionId(), e);
			statusReport.addFailureMessage(e.getMessage());
			statusReport.getReportSummary().put(SQL_SUMMARY_KEY, e.getMessage());
			return statusReport;
		} catch (SQLException e) {
			// Only the extension combine throws this; see runExtensionSplit.
			String message = ExceptionUtils.getExceptionCause("Failed to combine the extension with its dependency", e);
			LOGGER.error(message, e);
			statusReport.addFailureMessage(message);
			statusReport.getReportSummary().put(SQL_SUMMARY_KEY, message);
			return statusReport;
		}

		List<String> fsnSchemas = new ArrayList<>(List.of(PROSPECTIVE_SCHEMA));
		if (!releases.dependencies().isEmpty()) {
			fsnSchemas.add(DEPENDENCY_SCHEMA);
		}
		if (releases.previous() != null) {
			fsnSchemas.add(PREVIOUS_SCHEMA);
		}
		constructTestReport(statusReport, executionConfig, timeStart, items,
				new DuckFailuresExtractor(connection, qaResultTable, whitelistService, source::findAll,
						whitelistBatchSize, fsnLookupBatchSize, fsnSchemas),
				store);
		return statusReport;
	}

	/**
	 * What {@code <PREVIOUS>} binds to: the previous release; for a release the run declared
	 * first-time, the empty schema, as MySQL's {@code ValidationVersionLoader.loadPreviousVersion}
	 * binds one; otherwise nothing, and statements that need it are not run.
	 */
	private static String previousSchema(MysqlExecutionConfig executionConfig, ReleaseDirectories releases) {
		if (releases.previous() != null) {
			return PREVIOUS_SCHEMA;
		}
		return releases.previousEmpty() || executionConfig.isFirstTimeRelease() ? EMPTY_SCHEMA : null;
	}

	/** Every assertion against one schema: an international or edition release. */
	private List<TestRunItem> runSinglePhase(Connection connection, DuckStore store,
			MysqlExecutionConfig executionConfig, ReleaseDirectories releases, String previousSchema,
			String reportStorage, Selection selection, String version) {
		DuckDbAssertionExecutionService executionService = executionService(
				connection, store, executionConfig, releases, previousSchema, PROSPECTIVE_SCHEMA, version);
		executionService.prepareSchema();
		List<TestRunItem> items = new ArrayList<>(executeAssertions(executionService, selection.resources(), reportStorage));
		items.addAll(executeAssertions(executionService, selection.assertions(), reportStorage));
		return items;
	}

	/**
	 * An extension: release-type assertions against the extension alone, everything else
	 * against the extension combined with its dependency, as
	 * {@code MysqlValidationService.runExtensionReleaseValidation} does.
	 *
	 * <p>A release-type assertion asks whether the package changed correctly since its
	 * previous release, so it must see the extension's own files; every other assertion asks
	 * about a coherent terminology, so it must see the dependency too.
	 *
	 * <p>A failed combine aborts the run, where MySQL logs it and runs the remaining
	 * assertions against a partly combined schema, which reports them clean.
	 */
	private List<TestRunItem> runExtensionSplit(Connection connection, DuckStore store,
			MysqlExecutionConfig executionConfig, ReleaseDirectories releases, String previousSchema,
			String reportStorage, Selection selection, String version) throws SQLException {
		List<Assertion> releaseType = new ArrayList<>();
		List<Assertion> rest = new ArrayList<>();
		for (Assertion assertion : selection.assertions()) {
			String keywords = assertion.getKeywords();
			if (keywords != null && keywords.contains(RELEASE_TYPE_VALIDATION)) {
				releaseType.add(assertion);
			} else {
				rest.add(assertion);
			}
		}
		LOGGER.info("Extension validation: {} release-type assertions against the extension, "
				+ "{} against the extension combined with its dependency", releaseType.size(), rest.size());

		DuckDbAssertionExecutionService extensionPhase = executionService(
				connection, store, executionConfig, releases, previousSchema, PROSPECTIVE_SCHEMA, version);
		extensionPhase.prepareSchema();
		// Resource assertions build tables in the schema a phase addresses, so they run once
		// per phase, as MySQL runs them per phase.
		List<TestRunItem> items = new ArrayList<>(executeAssertions(extensionPhase, selection.resources(), reportStorage));
		items.addAll(executeAssertions(extensionPhase, releaseType, reportStorage));

		if (executionConfig.isStandAloneProduct()) {
			LOGGER.info("Stand-alone product - no dependency combine");
			items.addAll(executeAssertions(extensionPhase, rest, reportStorage));
			return items;
		}
		if (releases.dependencies().isEmpty()) {
			throw new SQLException("Extension validation needs a dependency release, none supplied");
		}

		combineExtensionWithDependency(connection, store);
		DuckDbAssertionExecutionService combinedPhase = executionService(
				connection, store, executionConfig, releases, previousSchema, COMBINED_SCHEMA, version);
		combinedPhase.prepareSchema();
		items.addAll(executeAssertions(combinedPhase, selection.resources(), reportStorage));
		items.addAll(executeAssertions(combinedPhase, rest, reportStorage));
		return items;
	}

	/**
	 * Several dependencies: {@link #DEPENDENCY_SCHEMA} holds a view per table over the union
	 * of their schemas, as {@code ReleaseDataManager.combineKnownVersions} inserts every
	 * dependency's rows into one schema. Each dependency keeps its own schema, which is what
	 * {@link #insertIntoProspectiveDeltaTables} reads per dependency.
	 */
	private static void unionDependencies(Connection connection, DuckStore store, Collection<String> schemas)
			throws SQLException {
		try (Statement st = connection.createStatement()) {
			st.execute("CREATE SCHEMA IF NOT EXISTS " + DEPENDENCY_SCHEMA);
			for (String table : new TreeMap<>(store.tableColumns()).keySet()) {
				st.execute("CREATE OR REPLACE VIEW " + DEPENDENCY_SCHEMA + "." + table + " AS "
						+ schemas.stream().map(schema -> "SELECT * FROM " + schema + "." + table)
								.collect(Collectors.joining(" UNION ALL ")));
			}
		}
		LOGGER.info("Dependency schema is the union of {}", schemas);
	}

	/**
	 * Delta-only validation: the prospective snapshot is the previous release's snapshot with
	 * this delta applied, as {@code ValidationVersionLoader} builds it on MySQL (copy the
	 * previous snapshot, delete every row the delta supersedes, insert the delta). With no
	 * previous release MySQL copies from an empty schema, so the snapshot is the delta.
	 *
	 * <p>Keyed on {@link #keyColumns} rather than on {@code id}, which the identifier table
	 * does not have.
	 */
	private void rebuildSnapshotFromPreviousAndDelta(Connection connection, DuckStore store,
			ReleaseDirectories releases) throws SQLException {
		long t0 = System.currentTimeMillis();
		Map<String, String> tableColumns = store.tableColumns();
		int rebuilt = 0;
		try (Statement st = connection.createStatement()) {
			for (String snapshot : snapshotTables(tableColumns, "_d")) {
				String delta = counterpart(snapshot, "_d");
				String join = keyColumns(snapshot, columnNames(tableColumns.get(snapshot))).stream()
						.map(c -> "s." + c + " = d." + c)
						.collect(Collectors.joining(" AND "));
				if (releases.previous() != null) {
					st.execute("INSERT INTO " + PROSPECTIVE_SCHEMA + "." + snapshot
							+ " SELECT * FROM " + PREVIOUS_SCHEMA + "." + snapshot);
				}
				st.execute("DELETE FROM " + PROSPECTIVE_SCHEMA + "." + snapshot + " s WHERE EXISTS "
						+ "(SELECT 1 FROM " + PROSPECTIVE_SCHEMA + "." + delta + " d WHERE " + join + ")");
				st.execute("INSERT INTO " + PROSPECTIVE_SCHEMA + "." + snapshot
						+ " SELECT * FROM " + PROSPECTIVE_SCHEMA + "." + delta);
				rebuilt++;
			}
		}
		LOGGER.info("Delta-only: rebuilt {} snapshot tables from {} and this delta, in {}ms", rebuilt,
				releases.previous() == null ? "no previous release" : "the previous release",
				System.currentTimeMillis() - t0);
	}

	/**
	 * A package with no Delta files: the delta tables derived as
	 * {@code ReleaseDataManager.insertIntoProspectiveDeltaTables} derives them, its three rules
	 * in its order.
	 * <ol>
	 * <li>An edition validated as an extension: each dependency's full rows newer than that
	 *     dependency's previous effective time.
	 * <li>A first-time release: the snapshot rows at this release's effective time (or blank).
	 *     Otherwise: the full rows newer than the previous release's effective time (or
	 *     blank; with no previous effective time, only blank ones).
	 * <li>For an edition validated as an extension, rule 2 leaves out rows the dependency's
	 *     full table already holds for the same component, module and effective time.
	 * </ol>
	 * Effective times are compared as stored: zero-padded {@code YYYYMMDD} sorts as the dates
	 * MySQL casts them to.
	 */
	private static void insertIntoProspectiveDeltaTables(Connection connection, DuckStore store,
			MysqlExecutionConfig executionConfig, Map<String, String> dependencySchemas) throws SQLException {
		Map<String, String> tableColumns = store.tableColumns();
		List<String> snapshots = snapshotTables(tableColumns, "_d", "_f");
		boolean editionOfExtension = executionConfig.isReleaseAsAnEdition() && executionConfig.isExtensionValidation();
		try (Statement st = connection.createStatement()) {
			if (editionOfExtension) {
				Map<String, String> previousTimes = executionConfig.getCurrentDependencyToPreviousEffectiveTimeMap();
				for (Map.Entry<String, String> dependency : dependencySchemas.entrySet()) {
					String previousTime = previousTimes == null ? null : previousTimes.get(dependency.getKey());
					for (String snapshot : snapshots) {
						st.execute(insertInto(PROSPECTIVE_SCHEMA, counterpart(snapshot, "_d"), tableColumns.get(snapshot),
								dependency.getValue() + "." + counterpart(snapshot, "_f"))
								+ (previousTime == null ? "" : " WHERE a." + EFFECTIVE_TIME + " > " + effectiveTime(previousTime)));
					}
				}
			}
			for (String snapshot : snapshots) {
				String columns = tableColumns.get(snapshot);
				String where;
				String from;
				if (executionConfig.isFirstTimeRelease()) {
					from = PROSPECTIVE_SCHEMA + "." + snapshot;
					String effectiveTime = executionConfig.getEffectiveTime() == null ? "" : executionConfig.getEffectiveTime();
					where = " WHERE (a." + EFFECTIVE_TIME + " IS NULL OR a." + EFFECTIVE_TIME + " = "
							+ effectiveTime(effectiveTime) + ")";
				} else {
					from = PROSPECTIVE_SCHEMA + "." + counterpart(snapshot, "_f");
					String previousTime = executionConfig.getPreviousEffectiveTime();
					// MySQL compares with cast('null' as datetime), which is NULL: no row is newer.
					where = " WHERE (a." + EFFECTIVE_TIME + " IS NULL"
							+ (previousTime == null ? "" : " OR a." + EFFECTIVE_TIME + " > " + effectiveTime(previousTime)) + ")";
				}
				if (editionOfExtension && !dependencySchemas.isEmpty()) {
					String full = counterpart(snapshot, "_f");
					String match = keyColumns(snapshot, columnNames(columns)).stream()
							.map(c -> "f." + c + " = a." + c)
							.collect(Collectors.joining(" AND "));
					where += " AND NOT EXISTS (SELECT 1 FROM " + DEPENDENCY_SCHEMA + "." + full + " f WHERE " + match
							+ " AND f.moduleid = a.moduleid AND f." + EFFECTIVE_TIME + " = a." + EFFECTIVE_TIME + ")";
				}
				st.execute(insertInto(PROSPECTIVE_SCHEMA, counterpart(snapshot, "_d"), columns, from) + where);
			}
		}
		LOGGER.info("No Delta files in the package: {} delta tables derived ({}{})", snapshots.size(),
				executionConfig.isFirstTimeRelease() ? "first-time release" : "from the full tables",
				editionOfExtension ? ", with each dependency's changes" : "");
	}

	/**
	 * A package with no Full files: each full table is its snapshot, as
	 * {@code ReleaseDataManager.insertIntoProspectiveFullTables} derives it.
	 */
	private static void insertIntoProspectiveFullTables(Connection connection, DuckStore store) throws SQLException {
		Map<String, String> tableColumns = store.tableColumns();
		List<String> snapshots = snapshotTables(tableColumns, "_f");
		try (Statement st = connection.createStatement()) {
			for (String snapshot : snapshots) {
				st.execute(insertInto(PROSPECTIVE_SCHEMA, counterpart(snapshot, "_f"), tableColumns.get(snapshot),
						PROSPECTIVE_SCHEMA + "." + snapshot));
			}
		}
		LOGGER.info("No Full files in the package: {} full tables derived from the snapshot", snapshots.size());
	}

	/** {@code INSERT INTO schema.table (columns) SELECT a.columns FROM source a}. */
	private static String insertInto(String schema, String table, String columnSpec, String source) {
		List<String> columns = columnNames(columnSpec);
		return "INSERT INTO " + schema + "." + table + " (" + String.join(", ", columns) + ") SELECT "
				+ columns.stream().map(c -> "a." + c).collect(Collectors.joining(", ")) + " FROM " + source + " a";
	}

	/** An effective time as a SQL literal, in the stored {@code YYYYMMDD} form. */
	private static String effectiveTime(String value) {
		return "'" + value.replace("-", "").replace("'", "") + "'";
	}

	/** The declared snapshot tables whose counterparts with these suffixes are declared too, sorted. */
	private static List<String> snapshotTables(Map<String, String> tableColumns, String... counterparts) {
		List<String> out = new ArrayList<>();
		for (String table : new TreeMap<>(tableColumns).keySet()) {
			if (table.endsWith("_s") && Stream.of(counterparts).allMatch(s -> tableColumns.containsKey(counterpart(table, s)))) {
				out.add(table);
			}
		}
		return out;
	}

	private static String counterpart(String snapshot, String suffix) {
		return snapshot.substring(0, snapshot.length() - 2) + suffix;
	}

	/**
	 * Builds {@link #COMBINED_SCHEMA}: the extension's own delta and full tables, and each
	 * snapshot merged with the dependency's.
	 *
	 * <p>The merge rule is {@code ReleaseDataManager.copyData}'s: one row per key, the highest
	 * effectiveTime, the dependency winning a tie. effectiveTime is compared as stored
	 * (zero-padded YYYYMMDD sorts chronologically), and the key is the table's own (see
	 * {@link #keyColumns}).
	 */
	private void combineExtensionWithDependency(Connection connection, DuckStore store) throws SQLException {
		long t0 = System.currentTimeMillis();
		int merged = 0;
		int copied = 0;
		try (Statement st = connection.createStatement()) {
			st.execute("CREATE SCHEMA IF NOT EXISTS " + COMBINED_SCHEMA);
			for (Map.Entry<String, String> entry : new TreeMap<>(store.tableColumns()).entrySet()) {
				String table = entry.getKey();
				if (table.endsWith("_s")) {
					st.execute(mergedSnapshotSql(table, columnNames(entry.getValue())));
					merged++;
				} else {
					st.execute("CREATE OR REPLACE TABLE " + COMBINED_SCHEMA + "." + table
							+ " AS SELECT * FROM " + PROSPECTIVE_SCHEMA + "." + table);
					copied++;
				}
			}
		}
		LOGGER.info("Combined extension with dependency: {} snapshot tables merged, {} delta/full tables copied, in {}ms",
				merged, copied, System.currentTimeMillis() - t0);
	}

	private static String mergedSnapshotSql(String table, List<String> columns) {
		String select = String.join(", ", columns);
		String key = String.join(", ", keyColumns(table, columns));
		String order = columns.contains(EFFECTIVE_TIME) ? EFFECTIVE_TIME + " DESC, " + MERGE_SOURCE : MERGE_SOURCE;
		return "CREATE OR REPLACE TABLE " + COMBINED_SCHEMA + "." + table + " AS SELECT " + select
				+ " FROM (SELECT *, 0 AS " + MERGE_SOURCE + " FROM " + DEPENDENCY_SCHEMA + "." + table
				+ " UNION ALL SELECT *, 1 AS " + MERGE_SOURCE + " FROM " + PROSPECTIVE_SCHEMA + "." + table
				+ ") QUALIFY row_number() OVER (PARTITION BY " + key + " ORDER BY " + order + ") = 1";
	}

	/**
	 * The columns that identify a component in a snapshot table: {@code id}, except for the
	 * RF2 Identifier file, keyed on {@code (identifierSchemeId, alternateIdentifier)}.
	 */
	private static List<String> keyColumns(String table, List<String> columns) {
		if (columns.contains("id")) {
			return List.of("id");
		}
		if (columns.contains("identifierschemeid") && columns.contains("alternateidentifier")) {
			return List.of("identifierschemeid", "alternateidentifier");
		}
		throw new IllegalStateException("No key columns known for " + table + " - cannot merge it with a dependency");
	}

	private static List<String> columnNames(String columnSpec) {
		List<String> names = new ArrayList<>();
		for (String column : columnSpec.split(",")) {
			String trimmed = column.trim();
			if (!trimmed.isEmpty()) {
				int space = trimmed.indexOf(' ');
				names.add((space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase(Locale.ROOT));
			}
		}
		return names;
	}

	private DuckDbAssertionExecutionService executionService(Connection connection, DuckStore store,
			MysqlExecutionConfig executionConfig, ReleaseDirectories releases, String previousSchema, String schema,
			String version) {
		boolean noDependency = releases.dependencies().isEmpty();
		DuckBinder binder = new DuckBinder(store.sentinels(), new DuckBinder.Config(
				executionConfig.getExecutionId(), schema, previousSchema,
				noDependency ? null : DEPENDENCY_SCHEMA,
				qaResultTable, executionConfig.getDefaultModuleId(),
				executionConfig.getIncludedModules(), version,
				noDependency ? EMPTY_SCHEMA : null));
		try {
			session(connection, schema);
		} catch (SQLException e) {
			throw new DuckDbAssertionExecutionService.SetupFailedException(
					List.of("SET search_path='" + schema + "' -> " + e.getMessage()));
		}
		return new DuckDbAssertionExecutionService(store, binder, connection);
	}

	/**
	 * The resource assertions and the requested groups' assertions, kept apart.
	 *
	 * <p>Resource assertions build intermediate tables other assertions read, in the schema a
	 * phase addresses, so each phase runs them first. They are excluded from the requested
	 * set: running one twice in a phase would insert its findings twice.
	 */
	private record Selection(List<Assertion> resources, List<Assertion> assertions) {

		int total() {
			return resources.size() + assertions.size();
		}
	}

	private static Selection selectAssertions(DuckAssertionSource source, MysqlExecutionConfig executionConfig) {
		List<Assertion> resources = new ArrayList<>(source.getAssertionsByKeyWords(RESOURCE_KEYWORD, true));
		LOGGER.info("Found total resource assertions need to be run before test {}", resources.size());

		Set<UUID> already = new LinkedHashSet<>();
		resources.forEach(a -> already.add(a.getUuid()));
		List<Assertion> selected = new ArrayList<>();
		List<String> groupNames = executionConfig.getGroupNames();
		if (!CollectionUtils.isEmpty(groupNames)) {
			for (Assertion assertion : source.getAssertionsInGroups(groupNames)) {
				if (already.add(assertion.getUuid())) {
					selected.add(assertion);
				}
			}
		}
		List<String> excluded = executionConfig.getAssertionExclusionList();
		if (!CollectionUtils.isEmpty(excluded)) {
			selected.removeIf(a -> excluded.contains(a.getUuid().toString()));
			resources.removeIf(a -> excluded.contains(a.getUuid().toString()));
		}
		return new Selection(resources, selected);
	}

	private List<TestRunItem> executeAssertions(DuckDbAssertionExecutionService executionService,
			List<Assertion> assertions, String reportStorage) {
		reportService.writeProgress(START_EXECUTING_ASSERTIONS, reportStorage);
		List<TestRunItem> results = new ArrayList<>();
		for (int from = 0; from < assertions.size(); from += PROGRESS_EVERY) {
			int to = Math.min(from + PROGRESS_EVERY, assertions.size());
			results.addAll(executionService.execute(assertions.subList(from, to)));
			reportService.writeProgress(String.format("[%1s] of [%2s] assertions are completed.", to, assertions.size()),
					reportStorage);
		}
		return results;
	}

	/**
	 * {@code MysqlValidationService.constructTestReport} with the DuckDB extractor, plus the
	 * provenance of the store that ran. As there, a failure count of -1 (not run) is reported
	 * both as incomplete and as failed.
	 */
	private void constructTestReport(ValidationStatusReport statusReport,
			MysqlExecutionConfig executionConfig, long timeStart, List<TestRunItem> items,
			DuckFailuresExtractor extractor, DuckStore store) {
		ValidationReport report = statusReport.getResultReport();
		try {
			report.setAssertionPacks(store.provenance().stream()
					.map(pack -> new ValidationReport.AssertionPackRecord(
							pack.name(), pack.version(), pack.digest(), pack.corpusRef(), pack.assertions()))
					.toList());
		} catch (IOException e) {
			// The run happened and its report is written, but it cannot claim an identity the
			// store failed to verify.
			LOGGER.error("The executed store cannot state its provenance, so this report carries none: {}",
					e.getMessage());
		}
		try {
			extractor.extractTestResults(items, executionConfig);

			final List<TestRunItem> failedItems = new ArrayList<>();
			final List<TestRunItem> warningItems = new ArrayList<>();
			final List<TestRunItem> incompleteItems = new ArrayList<>();
			for (final TestRunItem item : items) {
				item.setTestType(TestType.SQL);
				if (item.getFailureCount() != 0) {
					if (item.getFailureCount() == -1L) {
						incompleteItems.add(item);
					}
					if (SeverityLevel.WARN.toString().equalsIgnoreCase(item.getSeverity())) {
						warningItems.add(item);
					} else {
						failedItems.add(item);
					}
				}
			}
			report.addFailedAssertions(failedItems);
			report.addWarningAssertions(warningItems);
			report.addIncompleteAssesrtions(incompleteItems);
			items.removeAll(failedItems);
			items.removeAll(warningItems);
			report.addPassedAssertions(items);
		} catch (SQLException | RestClientException exception) {
			report.addFailedAssertions(Collections.emptyList());
			report.addWarningAssertions(Collections.emptyList());
			report.addPassedAssertions(Collections.emptyList());
			statusReport.addFailureMessage(ExceptionUtils.getExceptionCause("Failed to extract test results", exception));
		}
		report.addTimeTaken((System.currentTimeMillis() - timeStart) / 1000);
	}

	/**
	 * Puts a release into the run database, from the release cache where {@code cacheable}:
	 * the previous and dependency releases, which nothing writes to (assertions build their
	 * tables in the prospective or combined schema), and which do not change between runs.
	 */
	private void materialise(Connection connection, Path releaseDir, String schema, DuckStore store,
			boolean cacheable) throws SQLException, IOException {
		if (cacheable && releaseCache.isEnabled()) {
			Path cached = releaseCache.get(releaseDir, store.tableColumns());
			if (cached != null) {
				DuckReleaseCache.attach(connection, cached, schema);
				LOGGER.info("Attached cached release {} as schema '{}'", cached.getFileName(), schema);
				return;
			}
		}
		DuckMaterialiser.Result result = DuckMaterialiser.materialise(connection, releaseDir, schema, store.tableColumns());
		LOGGER.info("Materialised {} as schema '{}': {} tables, {} rows, {} empty files, {} placeholders",
				releaseDir, schema, result.tablesLoaded(), result.rows(), result.emptyFiles(), result.placeholders());
	}

	/** The search path, so unqualified table names resolve to the phase's schema. */
	private static void session(Connection connection, String schema) throws SQLException {
		try (Statement st = connection.createStatement()) {
			st.execute("SET search_path='" + schema + "'");
		}
	}

	private void createResultTable(Connection connection) throws SQLException {
		try (Statement st = connection.createStatement()) {
			st.execute("CREATE SCHEMA IF NOT EXISTS " + QA_RESULT_SCHEMA);
			st.execute("CREATE TABLE IF NOT EXISTS " + qaResultTable + " (" + QA_RESULT_COLUMNS + ")");
		}
	}

	/** The database file, its write-ahead log and its spill directory. */
	private static void deleteDatabase(Path database) {
		FileUtils.deleteQuietly(database.toFile());
		FileUtils.deleteQuietly(Path.of(database + ".wal").toFile());
		FileUtils.deleteQuietly(Path.of(database + ".tmp").toFile());
	}

	/**
	 * The value {@code <VERSION>} binds to: the effectiveTime suffix of the release's own RF2
	 * file names, as the MySQL engine reads it from the prospective schema name, which is
	 * built from the same suffix. The first in sorted order, so it is reproducible.
	 */
	static String releaseVersion(Path releaseDir) throws IOException {
		try (Stream<Path> walk = Files.walk(releaseDir)) {
			return walk.filter(Files::isRegularFile)
					.map(p -> p.getFileName().toString())
					.filter(name -> (name.contains("sct2_") || name.contains("der2_")) && name.endsWith(".txt"))
					.sorted()
					.map(RF2_VERSION::matcher)
					.filter(Matcher::matches)
					.map(m -> m.group(1))
					.findFirst()
					.orElse(null);
		}
	}

	private static List<String> rf2FileNames(Path releaseDir) throws IOException {
		return DuckMaterialiser.releaseFiles(releaseDir).values().stream()
				.flatMap(List::stream)
				.map(p -> p.getFileName().toString())
				.sorted()
				.toList();
	}
}
