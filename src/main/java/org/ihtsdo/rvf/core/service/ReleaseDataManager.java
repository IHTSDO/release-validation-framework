package org.ihtsdo.rvf.core.service;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.ihtsdo.rvf.core.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.core.service.config.ValidationMysqlBinaryStorageConfig;
import org.ihtsdo.rvf.core.service.config.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.core.service.util.MySqlDataTypeConverter;
import org.ihtsdo.rvf.core.service.util.RF2FileTableMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReleaseDataManager {

	public static final String RVF_DB_PREFIX = "rvf_";
	private static final String VERSION_NOT_FOUND = "version not found in RVF database ";
	private static final String ZIP_FILE_EXTENSION = ".zip";
	private static final Logger logger = LoggerFactory.getLogger(ReleaseDataManager.class);
	private static final Map<String, String> FILENAME_PATTERN_TO_EDITION_MAP = new HashMap<>();
	public static final String RECEIVING_RELEASE_DATA_INFO_MSG = "Receiving release data - {}";
	public static final String RELEASE_FILE_COPIED_TO_INFO_MSG = "Release file copied to : {}";
	public static final String COPY_RELEASE_FILE_WARNING_MSG = "Error copying release file to %s. Nested exception is : \n";
	public static final String SQL_SELECT = " SELECT * FROM ";
	public static final String SQL_ALTER_TABLE = "ALTER TABLE ";
	public static final String INSERT_INTO = "INSERT INTO ";

	static  {
		FILENAME_PATTERN_TO_EDITION_MAP.put("SpanishExtension.*_INT", "ES");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_NL_[0-9]+\\.txt", "NL");
		FILENAME_PATTERN_TO_EDITION_MAP.put("GB1000000_[0-9]+\\.txt", "UK");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_AU1000036_[0-9]+\\.txt", "AU");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_NZ1000210_[0-9]+\\.txt", "NZ");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_US1000124_[0-9]+\\.txt", "US");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_BE1000172_[0-9]+\\.txt", "BE");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_SE1000052_[0-9]+\\.txt", "SE");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_DK1000005_[0-9]+\\.txt", "DK");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_EE1000181_[0-9]+\\.txt", "EE");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_CH1000195_[0-9]+\\.txt", "CH");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_NO1000202_[0-9]+\\.txt", "NO");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_IE1000220_[0-9]+\\.txt", "IE");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_AT1000234_[0-9]+\\.txt", "AT");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_TM_[0-9]+\\.txt", "TM");
		FILENAME_PATTERN_TO_EDITION_MAP.put("_INT_[0-9]+\\.txt", "INT");
	}

	
	@Value("${rvf.data.folder.location}")
	private String sctDataLocation;
	
	private File sctDataFolder;
	
	@Value("${rvf.master.schema.name}")
	private String masterSchema;
	
	@Value("${rvf.jdbc.data.myisam.folder}")
	private String mysqlDataDir;
	
    @Resource(name = "dataSource")
	private BasicDataSource dataSource;
	
	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;
	
	private final Set<String> schemaNames = new HashSet<>();
	
	@Autowired
	private ResourceLoader cloudResourceLoader;

	@Autowired
	private ValidationReleaseStorageConfig releaseStorageConfig;
	
	@Autowired
	private ValidationMysqlBinaryStorageConfig mysqlBinaryStorageConfig;
	
	
	@PostConstruct
	public void init() {
		logger.info("Sct Data Location passed = {}", sctDataLocation);
		if (sctDataLocation == null || sctDataLocation.isEmpty()) {
			sctDataLocation = FileUtils.getTempDirectoryPath() + System.getProperty("file.separator") + "rvf-sct-data";
		}
		sctDataFolder = new File(sctDataLocation);
		if (!sctDataFolder.exists() && sctDataFolder.mkdirs()) {
			logger.info("Created data folder at : {}", sctDataLocation);
		}
		logger.info("Using data location as : {}", sctDataFolder.getAbsolutePath());
		fetchRvfSchemasFromDb();
	}

	/**
	 * Utility method that generates a map of all known releases based on the contents of the data folder.
	 */
	private void fetchRvfSchemasFromDb() {
		try ( ResultSet catalogs = dataSource.getConnection().getMetaData().getCatalogs()) {
			while (catalogs.next()) {
				 String schemaName = catalogs.getString(1);
				if (schemaName.startsWith(RVF_DB_PREFIX)) {
					schemaNames.add(schemaName);
				}
			}
		} catch (final SQLException e) {
			logger.error("Error getting list of existing schemas. Nested exception is : \n", e.fillInStackTrace());
		}
	}

	public void dropSchema(String schemaName) throws BusinessServiceException{
		logger.info("Dropping schema {}", schemaName);
		//clean database
		try (Statement statement = dataSource.getConnection().createStatement()) {
			String dropStr = "drop database if exists " + schemaName + ";";
			statement.execute(dropStr);
			schemaNames.remove(schemaName);
		} catch (SQLException e) {
			throw new BusinessServiceException("Failed to drop schema " + schemaName, e);
		}
	}
	
	public String getRVFVersion(String product, String releaseVersion) {
		return RVF_DB_PREFIX + product + "_" + releaseVersion;
	}

	/**
	 * Method that uses a {@link InputStream}  to copy a known/published release pack into the data folder.
	 * This method is not intended to be used
	 * for uploading prospective releases since they do not need to be stored for later use.
	 *
	 */
	public boolean uploadPublishedReleaseData(final InputStream inputStream, final String fileName, final String product, final String version) throws BusinessServiceException {
		// copy release pack zip to data location
		logger.info(RECEIVING_RELEASE_DATA_INFO_MSG, fileName);
		final File fileDestination = new File(sctDataFolder.getAbsolutePath(), fileName);
		try (OutputStream out = new FileOutputStream(fileDestination)) {
			IOUtils.copy(inputStream, out);
			logger.info(RELEASE_FILE_COPIED_TO_INFO_MSG, fileDestination.getAbsolutePath());
		} catch (final IOException e) {
			logger.warn(String.format(COPY_RELEASE_FILE_WARNING_MSG, sctDataFolder), e.fillInStackTrace());
			return false;
			
		} finally {
			IOUtils.closeQuietly(inputStream, null);
		}
		String rvfVersion = getRVFVersion(product, version);
		logger.info("RVF release version: {}", rvfVersion);
		if (schemaNames.contains(rvfVersion) ) {
			logger.info("Release version is already known in RVF and the existing one will be deleted and reloaded: {}", rvfVersion);
		}
		List<String> rf2FilesLoaded = new ArrayList<>();
		String schemaName = loadSnomedData(rvfVersion, rf2FilesLoaded, fileDestination);
		logger.info("schemaName = {}", schemaName);
		schemaNames.add(schemaName);
		return true;
	}
	
	
	public boolean uploadReleaseDataIntoDB(final InputStream inputStream, String fileName, String schemaName) throws BusinessServiceException {
		// copy release pack zip to data location
		logger.info(RECEIVING_RELEASE_DATA_INFO_MSG, fileName);
		final File fileDestination = new File(sctDataFolder.getAbsolutePath(), fileName);
		try (OutputStream out = new FileOutputStream(fileDestination)) {
			IOUtils.copy(inputStream, out);
			logger.info(RELEASE_FILE_COPIED_TO_INFO_MSG, fileDestination.getAbsolutePath());
		} catch (final IOException e) {
			logger.warn(String.format(COPY_RELEASE_FILE_WARNING_MSG, sctDataFolder), e.fillInStackTrace());
			return false;
			
		} finally {
			IOUtils.closeQuietly(inputStream, null);
		}
		
		if (schemaNames.contains(schemaName)) {
			logger.info("Release version is already known in RVF and the existing one will be deleted and reloaded: {}", schemaName);
		}
		logger.info("Loading data into schema {}", schemaName);
		List<String> rf2FilesLoaded = new ArrayList<>();
		loadSnomedData(schemaName, rf2FilesLoaded, fileDestination);
		logger.info("schemaName = {}", schemaName);
		return true;
	}

	public boolean uploadPublishedReleaseData(final File releasePackZip, final String product, final String version) throws BusinessServiceException {
		boolean result = false;
		try(InputStream inputStream = new FileInputStream(releasePackZip)) {
			 result = uploadPublishedReleaseData(inputStream, releasePackZip.getName(), product, version);
		} catch (final IOException e) {
			logger.error("Error during upload release:" + releasePackZip.getName(), e);
		}
		return result;
	}
	
	public String loadSnomedData(final String versionName, List<String> rf2FilesLoaded, final File... zipDataFile) throws BusinessServiceException {
		File outputFolder = null;
		final String createdSchemaName = versionName.startsWith(RVF_DB_PREFIX) ? versionName : RVF_DB_PREFIX + versionName;
		final long startTime = Calendar.getInstance().getTimeInMillis();
		try {
			outputFolder = new File(FileUtils.getTempDirectoryPath(), createdSchemaName);
			logger.info("Setting output folder location = {}", outputFolder.getAbsolutePath());
			if (Files.deleteIfExists(outputFolder.toPath())) {
				logger.info("Output folder already exists and will be deleted before recreating.");
			}
			if (outputFolder.mkdir()) {
				logger.info("Output folder created successfully.");
			} else {
				logger.error("Failed to create output folder.");
			}
			// extract SNOMED CT content from zip file
			for (final File zipFile : zipDataFile) {
				ZipFileUtils.extractFilesFromZipToOneFolder(zipFile, outputFolder.getAbsolutePath());
			}
			createSchema(createdSchemaName);
			loadReleaseFilesToDB(outputFolder, rvfDynamicDataSource, rf2FilesLoaded, createdSchemaName);
		} catch (final RVFExecutionException | IOException e) {
			List<String> fileNames = Arrays.stream(zipDataFile).map(File::getName).toList();
			final String errorMsg = String.format("Error while loading file %s into version %s", Arrays.toString(fileNames.toArray()), versionName);
			throw new BusinessServiceException(errorMsg, e);
		}  finally {
			// remove output directory so it does not occupy space
			FileUtils.deleteQuietly(outputFolder);
		}
		logger.info("Finished loading of data in : {} seconds.", ((Calendar.getInstance().getTimeInMillis() - startTime) / 1000));
		return createdSchemaName;
	}

	private void loadReleaseFilesToDB(final File rf2TextFilesDir, final RvfDynamicDataSource dataSource, List<String> rf2FilesLoaded, String schemaName) throws RVFExecutionException {
		if (rf2TextFilesDir != null) {
			final String[] rf2Files = rf2TextFilesDir.list((dir, name) ->
					name.endsWith(".txt") && (name.startsWith("der2") || name.startsWith("sct2") || name.startsWith("xder2") || name.startsWith("xsct2"))
			);
			if (rf2Files != null && rf2Files.length > 0) {
				final ReleaseFileDataLoader dataLoader = new ReleaseFileDataLoader(dataSource, schemaName, new MySqlDataTypeConverter());
				dataLoader.loadFilesIntoDB(rf2TextFilesDir.getAbsolutePath(), rf2Files, rf2FilesLoaded);
			}
		}
	}

	public boolean isKnownRelease(String releaseVersion) {
		if (releaseVersion == null || releaseVersion.endsWith(ZIP_FILE_EXTENSION)) {
			return false;
		}
		return schemaNames.contains(releaseVersion);
	}

	/**
	 * Returns a list of all known releases that have been uploaded into the database
	 * @return set of all releases
	 */
	public Set<String> getAllKnownReleases() {
		return this.schemaNames;
	}

	public void setSctDataLocation(final String sctDataLocationX) {
		sctDataLocation = sctDataLocationX;
	}
	
	public List<File> getZipFileForKnownRelease(final String knownVersion) {
		List<File> filesFound = new ArrayList<>();
		if (knownVersion != null ) {
			final File [] zipFiles = sctDataFolder.listFiles((dir, name) -> {
                final String[] tokens = name.split("_");
                final String lastToken = tokens[tokens.length -1];
                return lastToken.endsWith(ZIP_FILE_EXTENSION) && lastToken.contains(knownVersion);
            });
			if (zipFiles != null && zipFiles.length > 0) {
				filesFound.addAll(Arrays.asList(zipFiles));
			}
		}
		return filesFound;
	}
	
	
	
	public boolean combineKnownVersions(final String combinedVersionName, final String ... knownVersions){
		final long startTime = System.currentTimeMillis();
		logger.info("Combining known versions into {}", combinedVersionName);
		boolean isFailed = false;
		//create db schema for the combined version
		final String schemaName = RVF_DB_PREFIX + combinedVersionName;
		try {
			createSchema(schemaName);
		} catch (Exception e) {
			isFailed = true;
			logger.error(String.format("Failed to create db schema and tables for version: %s due to ", combinedVersionName), e.fillInStackTrace());
		}
		//select data from known version schema and insert into the new schema
		for (String knownSchema : knownVersions) {
			if (isKnownRelease(knownSchema)) {
				isFailed = true;
				logger.error("Known schema doesn't exist for: {}", knownSchema);
				break;
			}
			logger.info("Adding known version {} to schema {}", knownSchema, combinedVersionName);
			for (final String tableName : getValidTableNamesFromSchema(knownSchema, null)) {
				try {
					copyData(tableName, knownSchema, schemaName);
				} catch (BusinessServiceException e) {
					logger.error(" Copy data failed.", e);
					isFailed = true;
				}
			}
		}
		final long endTime = System.currentTimeMillis();
		logger.info("Time taken to combine both known versions into one schema in seconds: {}", ((endTime-startTime)/1000));
		return !isFailed;
	}
	

	/** Combine data from source schema A and B into target schema. Choose the most recent when both source schema A and B share the same ids.
	 * @param tableName the table name to copy from source to target schema
	 * @param sourceSchemaA 
	 * @param sourceSchemaB
	 * @param targetSchema
	 * @throws BusinessServiceException
	 */
	private void copyData(String tableName, String sourceSchemaA, String sourceSchemaB, String targetSchema) throws BusinessServiceException {
		final String disableIndex = SQL_ALTER_TABLE + tableName + " DISABLE KEYS";
		final String enableIndex = SQL_ALTER_TABLE + tableName + " ENABLE KEYS";
		String selectAFrom = "select a.* from ";
		String selectDataFromASql = selectAFrom + sourceSchemaA + "." + tableName + " a where not exists ( select c.id from " + sourceSchemaB + "."
				+ tableName + " c where a.id=c.id)";
		String latestDataFromASelectSql = selectAFrom + sourceSchemaA + "." + tableName + " a where exists ( select b.id from " + sourceSchemaB + "." + tableName
					+ " b where a.id=b.id and cast(a.effectivetime as datetime) >= cast(b.effectivetime as datetime))";
		
		String selectDataFromBSql = selectAFrom + sourceSchemaB + "." + tableName + " a where not exists ( select c.id from " + sourceSchemaA + "."
				+ tableName + " c where a.id=c.id)";
		String latestDataFromBSelectSql = selectAFrom + sourceSchemaB + "." + tableName + " a where exists ( select b.id from " + sourceSchemaA + "." + tableName
					+ " b where a.id=b.id and cast(a.effectivetime as datetime) > cast(b.effectivetime as datetime))";
		
		final String insertSql = INSERT_INTO + targetSchema + "." + tableName  + " ";
		logger.debug("Copying table {}", tableName);
		try (Connection connection = rvfDynamicDataSource.getConnection(targetSchema);
			Statement statement = connection.createStatement() ) {
			statement.execute(disableIndex);
			statement.execute(insertSql + selectDataFromASql);
			statement.execute(insertSql + selectDataFromBSql);
			statement.execute(insertSql + latestDataFromASelectSql);
			statement.execute(insertSql + latestDataFromBSelectSql);
			statement.execute(enableIndex);
		} catch (final SQLException e) {
			throw new BusinessServiceException("Failed to insert data to table: " + tableName, e);
		}
	}

	private void copyData(String tableName, String sourceSchema, String targetSchema) throws BusinessServiceException {
		final String disableIndex = SQL_ALTER_TABLE + tableName + " DISABLE KEYS";
		final String enableIndex = SQL_ALTER_TABLE + tableName + " ENABLE KEYS";
		final String sql = "insert into " + targetSchema + "." + tableName  + SQL_SELECT + sourceSchema + "." + tableName;
		logger.debug("Copying table {} with sql {} ", tableName, sql);
		try (Connection connection = rvfDynamicDataSource.getConnection(targetSchema);
			Statement statement = connection.createStatement() ) {
			statement.execute(disableIndex);
			statement.execute(sql);
			statement.execute(enableIndex);
		} catch (final SQLException e) {
			String msg = "Failed to insert data to table: " + tableName;
			throw new BusinessServiceException(msg, e);
		}		
	}

	private List<String> getValidTableNamesFromSchema(String schemaName, String tableNamePattern) {
		List<String> result = new ArrayList<>();
		Collection<String> mappedTables = RF2FileTableMapper.getAllTableNames();
		String sql = "select table_name from INFORMATION_SCHEMA.TABLES WHERE table_schema =?";
		if (tableNamePattern != null) {
			sql = sql + " and table_name like ?";
		}
		try (Connection connection = rvfDynamicDataSource.getConnection(schemaName);
			PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, schemaName);
			if (tableNamePattern != null) {
				statement.setString(2, tableNamePattern);
			}
			ResultSet resultSet = statement.executeQuery();
			while( resultSet.next()) {
				String tableName = resultSet.getString(1);
				if (mappedTables.contains(tableName)) {
					result.add(tableName);
				}
			}
		} catch (SQLException e) {
			logger.error(String.format("Failed to select table name from db schema: %s due to ",schemaName), e.fillInStackTrace());
		}
		return result;
	}

	public void copyTableData(String sourceVersion, String destinationVersion, 
			String tableNamePattern, List<String> excludeTableNames) throws BusinessServiceException {
		final long startTime = System.currentTimeMillis();
		StringBuilder errorMsg = new StringBuilder();
		if (!isKnownRelease(sourceVersion)) {
			errorMsg.append(VERSION_NOT_FOUND).append(sourceVersion);
		}
		if (!isKnownRelease(destinationVersion)) {
			errorMsg.append(VERSION_NOT_FOUND).append(destinationVersion);
		}
		if (!errorMsg.isEmpty()) {
			throw new BusinessServiceException(errorMsg.toString());
		}
		for (final String tableName : getValidTableNamesFromSchema(sourceVersion, tableNamePattern)) {
			if (excludeTableNames != null && excludeTableNames.contains(tableName)) {
				continue;
			}
			copyData(tableName, sourceVersion, destinationVersion);
		}
		final long endTime = System.currentTimeMillis();
		logger.info("Copy data with table name like {} from {} into {} completed in seconds {} ", 
				tableNamePattern, sourceVersion, destinationVersion, (endTime-startTime)/1000);
	}

	public void updateSnapshotTableWithDataFromDelta(String schema) {
		List<String> deltaTableNames = getValidTableNamesFromSchema(schema, "%_d");
		for (String deltaTbl : deltaTableNames) {
			String snapshotTbl = deltaTbl.replace("_d", "_s");
			try (Connection connection = rvfDynamicDataSource.getConnection(schema);
					Statement statement = connection.createStatement() ) {
				final String deleteSql = "delete a.* from " + schema + "." + snapshotTbl + " a where exists ( select b.id from " + schema + "." + deltaTbl + " b where a.id=b.id)";
				logger.debug("Delete data from snapshot table sql: {}", deleteSql);
				final String insertSql = "insert into " + schema + "." + snapshotTbl  + SQL_SELECT + schema + "." + deltaTbl;
				logger.debug("Insert delta into snapshot table sql: {}", insertSql);
				statement.execute(deleteSql);
				statement.execute(insertSql);
			} catch (final SQLException e) {
				logger.error("Failed to update table {} with data from {}  due to {} ", snapshotTbl, deltaTbl, e.getMessage(), e);
			}
		}
		
	}

	public void copyTableData(String sourceSchemaA, String sourceSchemaB, String destinationSchema, String tableNamePattern,
			List<String> excludeTableNames) throws BusinessServiceException {
		final long startTime = System.currentTimeMillis();
		StringBuilder errorMsg = new StringBuilder();
		if (!isKnownRelease(sourceSchemaA)) {
			errorMsg.append(VERSION_NOT_FOUND).append(sourceSchemaA);
		}
		if (!isKnownRelease(sourceSchemaB)) {
			errorMsg.append(VERSION_NOT_FOUND).append(sourceSchemaB);
		}
		if (!isKnownRelease(destinationSchema)) {
			errorMsg.append(VERSION_NOT_FOUND).append(destinationSchema);
		}
		if (!errorMsg.isEmpty()) {
			throw new BusinessServiceException(errorMsg.toString());
		}
		for (String tableName : getValidTableNamesFromSchema(sourceSchemaA, tableNamePattern)) {
			if (excludeTableNames != null && excludeTableNames.contains(tableName)) {
				continue;
			}
			copyData(tableName, sourceSchemaA, sourceSchemaB, destinationSchema);
		}
		final long endTime = System.currentTimeMillis();
		logger.info("Copy data with table name like {} from {} {} into {} completed in seconds {} ", 
				tableNamePattern, sourceSchemaA, sourceSchemaB, destinationSchema, (endTime-startTime)/1000);
	}

	public String createSchema(String version) throws BusinessServiceException {
		String schemaName = version.startsWith(RVF_DB_PREFIX) ? version : RVF_DB_PREFIX + version;
		logger.info("Creating db schema {}", schemaName);
		//clean and create database
		try (Statement statement = dataSource.getConnection().createStatement()) {
			String dropStr = "drop database if exists " + schemaName + ";";
			String createDbStr = "create database if not exists "+ schemaName + ";";
			statement.execute(dropStr);
			statement.execute(createDbStr);
		} catch (SQLException e) {
			throw new BusinessServiceException("Failed to create schema " + schemaName, e);
		}

		try {
			try (Statement statement = rvfDynamicDataSource.getConnection(schemaName).createStatement()) {
				statement.execute("use " + schemaName + ";");
			} 
			try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/sql/create-tables-mysql.sql"));
				 Connection connection = rvfDynamicDataSource.getConnection(schemaName)) {
				ScriptRunner runner = new ScriptRunner(connection);
				runner.runScript(reader);
			} 
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to create tables for schema " + schemaName, e);
		}
		schemaNames.add(schemaName);
		logger.info("{} is created successfully.", schemaName);
		return schemaName;
	}

	public void clearQAResult(Long runId) {
		String deleteQaResultSQL = " delete from rvf_master.qa_result where run_id = " + runId;
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement() ) {
			statement.execute(deleteQaResultSQL);
		} catch (final SQLException e) {
			logger.error("Failed to delete data from qa_result table for runId {} due to {} ", runId, e.getMessage(), e);
		}
	}

	public void truncateQAResult() {
		String truncateQaResultSQL = " truncate table rvf_master.qa_result";
		try (Connection connection = dataSource.getConnection();
			 Statement statement = connection.createStatement() ) {
			statement.execute(truncateQaResultSQL);
		} catch (final SQLException e) {
			logger.error("Failed to truncate qa_result table due to {} ", e.getMessage(), e);
		}
	}
	
	public String generateBinaryArchive(String schemaName) throws BusinessServiceException {
		if (schemaName == null || !isKnownRelease(schemaName)) {
			throw new IllegalArgumentException("No schema found for " + schemaName);
		}
		File dataDir = new File(mysqlDataDir);
		File binaryFile = new File(mysqlDataDir, schemaName);
		if (!dataDir.canRead()) {
			logger.error("Can't access directory {}", dataDir.getPath());
			try {
				GroupPrincipal group = Files.readAttributes(dataDir.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
				UserPrincipal owner = Files.readAttributes(dataDir.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).owner();
				logger.info("user group: {}", group);
				logger.info("owner : {}", owner);
				Files.setOwner(binaryFile.toPath(), owner);
				Files.getFileAttributeView(binaryFile.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
				Files.getFileAttributeView(binaryFile.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(owner);
			} catch (IOException e) {
				throw new BusinessServiceException("Failed to fetch group principal for folder " + dataDir, e.fillInStackTrace());
			}
		}
		if (!binaryFile.exists()) {
			throw new BusinessServiceException("No mysql binary file found for " + binaryFile.getPath());
		}
	
		File archiveFile = new File(FileUtils.getTempDirectoryPath(), schemaName + ZIP_FILE_EXTENSION);
		try {
			ZipFileUtils.zip(binaryFile.getAbsolutePath(), archiveFile.getAbsolutePath());
			logger.info("Mysql binary archive file is created {}", archiveFile.getPath());
			ResourceManager resourceManager = new ResourceManager(mysqlBinaryStorageConfig, cloudResourceLoader);
			resourceManager.writeResource(archiveFile.getName(), new FileInputStream(archiveFile));
			logger.info("Mysql binary archive file {} is loaded to {}", archiveFile.getName(), mysqlBinaryStorageConfig);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to zip binary file " + binaryFile.getAbsolutePath(), e);
		}
		return archiveFile.getName();
	}

	public boolean restoreReleaseFromBinaryArchive(String schemaName) throws IOException, BusinessServiceException {
		File outputFile = null;
		try {
			ResourceManager resourceManager = new ResourceManager(mysqlBinaryStorageConfig, cloudResourceLoader);
			String archiveFileName = schemaName + ZIP_FILE_EXTENSION;
			InputStream inputStream = resourceManager.readResourceStreamOrNullIfNotExists(archiveFileName);
			if (inputStream == null) {
				logger.info("No resource available for {} via {}", archiveFileName, mysqlBinaryStorageConfig);
				return false;
			}
			outputFile = downloadFile(inputStream, archiveFileName);
			if (outputFile == null) {
				logger.error("Failed to download {} via {}", archiveFileName, mysqlBinaryStorageConfig);
				return false;
			}
			// In mysql 8 you must create database schema and tables as restoring binary files alone doesn't create schema and tables
			createSchema(schemaName);
			// Restore data from binary archive
			File outputDir = new File(mysqlDataDir, schemaName);
			logger.info("Extracting mysql binary files from {} to {}", outputFile.getPath(), outputDir.getPath());
			ZipFileUtils.extractFilesFromZipToOneFolder(outputFile, outputDir.getAbsolutePath());
			logger.info("Mysql binary files are restored successfully in {}",  outputDir.getPath());
			fetchRvfSchemasFromDb();
			return true;
		} finally {
			if (outputFile != null) {
				FileUtils.deleteQuietly(outputFile);
			}
		}
	}

	public boolean uploadPublishedReleaseFromStore(String releaseFilename, String schemaName) throws BusinessServiceException {
		// check local disk first
		if (!releaseStorageConfig.isUseCloud()) {
			File uploadedFile = new File(sctDataFolder, releaseFilename);
			if (uploadedFile.exists() && uploadedFile.canRead()) {
				loadSnomedData(schemaName, new ArrayList<>(), uploadedFile);
				return true;
			}
		}
		InputStream inputStream;
		try {
			ResourceManager resourceManager = new ResourceManager(releaseStorageConfig, cloudResourceLoader);
			inputStream = resourceManager.readResourceStream(releaseFilename);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to read file " + releaseFilename + " via " + releaseStorageConfig.toString(), e);
		}
		uploadReleaseDataIntoDB(inputStream, releaseFilename, schemaName);
		return true;
	}

	private File downloadFile(InputStream input, String outputFilename) {
		final File fileDestination = new File(sctDataFolder.getAbsolutePath(), outputFilename);
		try (OutputStream out = new FileOutputStream(fileDestination)) {
			IOUtils.copy(input, out);
			logger.info(RELEASE_FILE_COPIED_TO_INFO_MSG, fileDestination.getAbsolutePath());
			return fileDestination;
		} catch (final IOException e) {
			logger.warn(String.format(COPY_RELEASE_FILE_WARNING_MSG, sctDataFolder), e.fillInStackTrace());
			return null;
		} finally {
			IOUtils.closeQuietly(input, null);
		}
	}

	public String getEditionAndVersion(final File zipDataFile)  throws BusinessServiceException {
		String editionAndVersion = "";
		String snomedFile = "";
		List<String> zipFileList = getFileList(zipDataFile);
		Optional<String> sctOrDerFile = zipFileList.stream()
							.filter(file -> (file.contains("sct2_")
										|| file.contains("der2_"))
										&& file.endsWith(".txt"))
							.findFirst();
		if (sctOrDerFile.isEmpty()) {
			throw new BusinessServiceException("There are no RF2 files in data file: " + zipDataFile);
		}
		snomedFile = sctOrDerFile.get();
		Matcher matcher = Pattern.compile(".*_(\\d+)\\.txt").matcher(snomedFile);
		if (matcher.matches()) {
			editionAndVersion = 
				mapFilenameToEdition(snomedFile).toLowerCase() + "_"
				+ matcher.group(1);
		} else {
			throw new BusinessServiceException(
				"Could not find RF2 file with standard name in data zip file " 
				+ zipDataFile.getName());
		}
		logger.info ("Identified edition and version {} from zip file {}", editionAndVersion, zipDataFile.getName());
		return editionAndVersion;
	}

	private String mapFilenameToEdition(String name) {
		String edition = "INT";
		for (Map.Entry<String, String> entry : FILENAME_PATTERN_TO_EDITION_MAP.entrySet()) {
			if (Pattern.compile(entry.getKey()).matcher(name).find()) {
				return entry.getValue();
			}
		}
		return edition;
	}

	private List<String> getFileList(final File dataFile) throws BusinessServiceException {
		try {
			return ZipFileUtils.listFiles(dataFile);
		} catch (IOException e) {
			throw new BusinessServiceException("Could not get file list from " + dataFile, e);
		}
	}

	public void insertIntoProspectiveDeltaTables(String schemaName, MysqlExecutionConfig executionConfig)  throws SQLException  {
		try (Connection connection = rvfDynamicDataSource.getConnection(schemaName)) {
			DatabaseMetaData md = connection.getMetaData();
			ResultSet rs = md.getTables(null, null, "%", null);
			Set<String> snapShotTables = new HashSet<>();
			while (rs.next()) {
				if (rs.getString(3).endsWith("_s")) {
					snapShotTables.add(rs.getString(3));
				}
			}
			if (StringUtils.isNotEmpty(executionConfig.getPreviousDependencyEffectiveTime()) && StringUtils.isNotEmpty(executionConfig.getExtensionDependencyVersion())) {
				insertIntoProspectiveDeltaTablesFromDependency(executionConfig, snapShotTables, connection);
			}
			if (executionConfig.isFirstTimeRelease()) {
				insertIntoProspectiveDeltaTablesForFirstTimeRelease(executionConfig, snapShotTables, connection);
			} else {
				insertIntoProspectiveDeltaTablesForNoneFirstTimeRelease(executionConfig, snapShotTables, connection);
			}
		}
	}

	private static void insertIntoProspectiveDeltaTablesForNoneFirstTimeRelease(MysqlExecutionConfig executionConfig, Set<String> snapShotTables, Connection connection) throws SQLException {
		for (String snapshotTable: snapShotTables) {
			String insertSQL = INSERT_INTO + snapshotTable.replaceAll("_s$","_d")
					+ SQL_SELECT + snapshotTable.replaceAll("_s$", "_f") + " a"
					+ " WHERE (a.effectivetime IS NULL OR cast(a.effectivetime as datetime) > cast('" + executionConfig.getPreviousEffectiveTime() + "' as datetime))";
			if (StringUtils.isNotEmpty(executionConfig.getPreviousDependencyEffectiveTime()) && StringUtils.isNotEmpty(executionConfig.getExtensionDependencyVersion())) {
				insertSQL += " AND NOT EXISTS (SELECT id FROM " + executionConfig.getExtensionDependencyVersion() + "." + snapshotTable.replaceAll("_s$", "_f") + " WHERE a.id = id AND a.moduleid = moduleid AND a.effectivetime = effectivetime)";
			}
			try (PreparedStatement ps = connection.prepareStatement(insertSQL)) {
				logger.info(insertSQL);
				ps.execute();
			}
		}
	}

	private void insertIntoProspectiveDeltaTablesForFirstTimeRelease(MysqlExecutionConfig executionConfig, Set<String> snapShotTables, Connection connection) throws SQLException {
		String effectiveTime = StringUtils.isNotBlank(executionConfig.getEffectiveTime()) ? executionConfig.getEffectiveTime().replace("-", "") : "";
		for (String snapshotTable : snapShotTables) {
			String insertSQL = INSERT_INTO + snapshotTable.replaceAll("_s$", "_d")
					+ SQL_SELECT + snapshotTable + " a"
					+ " WHERE (a.effectivetime IS NULL OR a.effectivetime=?)";
			if (StringUtils.isNotEmpty(executionConfig.getPreviousDependencyEffectiveTime()) && StringUtils.isNotEmpty(executionConfig.getExtensionDependencyVersion())) {
				insertSQL += " AND NOT EXISTS (SELECT id FROM " + executionConfig.getExtensionDependencyVersion() + "." + snapshotTable.replaceAll("_s$", "_f") + " WHERE a.id = id AND a.moduleid = moduleid AND a.effectivetime = effectivetime)";
			}
			try (PreparedStatement ps = connection.prepareStatement(insertSQL)) {
				ps.setString(1, effectiveTime);
				logger.info(insertSQL);
				ps.execute();
			}
		}
	}

	private void insertIntoProspectiveDeltaTablesFromDependency(MysqlExecutionConfig executionConfig, Set<String> snapShotTables, Connection connection) throws SQLException {
		String insertSQL;
		String previousDependencyEffectiveTime = executionConfig.getPreviousDependencyEffectiveTime().replace("-", "");
		for (String snapshotTable : snapShotTables) {
			insertSQL = INSERT_INTO + snapshotTable.replaceAll("_s$", "_d")
					+ SQL_SELECT + executionConfig.getExtensionDependencyVersion() + "." + snapshotTable.replaceAll("_s$", "_f") + " a"
					+ " WHERE cast(a.effectivetime as datetime) > cast('" + previousDependencyEffectiveTime + "' as datetime)";
			try (PreparedStatement ps = connection.prepareStatement(insertSQL)) {
				logger.info(insertSQL);
				ps.execute();
			}
		}
	}

	public long getPublishedReleaseLastModifiedDate(String publishedRelease) {
		ResourceManager resourceManager = new ResourceManager(releaseStorageConfig, cloudResourceLoader);
        try {
            return resourceManager.getResourceLastModifiedDate(publishedRelease);
        } catch (Exception e) {
            logger.warn("Failed to find the last modified for resource {}", publishedRelease, e);
			return 0L;
        }
    }

	public long getBinaryArchiveSchemaLastModifiedDate(String schemaName) {
		ResourceManager resourceManager = new ResourceManager(mysqlBinaryStorageConfig, cloudResourceLoader);
		String archiveFileName = schemaName + ZIP_FILE_EXTENSION;
		try {
			return resourceManager.getResourceLastModifiedDate(archiveFileName);
		} catch (Exception e) {
			logger.warn("Failed to find the last modified for resource {}", schemaName, e);
			return 0L;
		}
	}

	public void insertIntoProspectiveFullTables(String schemaName) throws SQLException {
		try (Connection connection = rvfDynamicDataSource.getConnection(schemaName)) {
			DatabaseMetaData md = connection.getMetaData();
			ResultSet rs = md.getTables(null, null, "%", null);
			Set<String> snapShotTables = new HashSet<>();
			while (rs.next()) {
				if (rs.getString(3).endsWith("_s")) {
					snapShotTables.add(rs.getString(3));
				}
			}
			insertIntoProspectiveFullTables(snapShotTables, connection);
		}
	}

	private void insertIntoProspectiveFullTables(Set<String> snapShotTables, Connection connection) throws SQLException {
		for (String snapshotTable: snapShotTables) {
			String fullTable = snapshotTable.replaceAll("_s$","_f");
			StringBuilder insertSQL = new StringBuilder();
			insertSQL.append(INSERT_INTO).append(fullTable).append(SQL_SELECT).append(snapshotTable);
			try (PreparedStatement ps = connection.prepareStatement(insertSQL.toString())) {
				ps.execute();
			}
		}
	}
}
