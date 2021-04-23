package org.ihtsdo.rvf.execution.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.config.ValidationMysqlBinaryStorageConfig;
import org.ihtsdo.rvf.execution.service.config.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.execution.service.util.MySqlDataTypeConverter;
import org.ihtsdo.rvf.execution.service.util.RF2FileTableMapper;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class ReleaseDataManager {

	public static final String RVF_DB_PREFIX = "rvf_";
	private static final String VERSION_NOT_FOUND = "version not found in RVF database ";
	private static final String ZIP_FILE_EXTENSION = ".zip";
	private static final Logger logger = LoggerFactory.getLogger(ReleaseDataManager.class);
	
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
	public void init() throws Exception {
		logger.info("Sct Data Location passed = " + sctDataLocation);
		if (sctDataLocation == null || sctDataLocation.isEmpty()) {
			sctDataLocation = FileUtils.getTempDirectoryPath() + System.getProperty("file.separator") + "rvf-sct-data";
		}
		sctDataFolder = new File(sctDataLocation);
		if (!sctDataFolder.exists()) {
			if (sctDataFolder.mkdirs()) {
				logger.info("Created data folder at : " + sctDataLocation);
			} 
		}
		logger.info("Using data location as :" + sctDataFolder.getAbsolutePath());
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
			logger.error("Error getting list of existing schemas. Nested exception is : \n" + e.fillInStackTrace());
		}
	}
	
	public String getRVFVersion(String product, String releaseVersion) {
		return RVF_DB_PREFIX + product + "_" + releaseVersion;
	}

	/**
	 * Method that uses a {@link java.io.InputStream}  to copy a known/published release pack into the data folder.
	 * This method is not intended to be used
	 * for uploading prospective releases since they do not need to be stored for later use.
	 *
	 */
	public boolean uploadPublishedReleaseData(final InputStream inputStream, final String fileName, final String product, final String version) throws BusinessServiceException {
		// copy release pack zip to data location
		logger.info("Receiving release data - " + fileName);
		final File fileDestination = new File(sctDataFolder.getAbsolutePath(), fileName);
		OutputStream out = null;
		try {
			out = new FileOutputStream(fileDestination);
			IOUtils.copy(inputStream, out);
			logger.info("Release file copied to : " + fileDestination.getAbsolutePath());
		} catch (final IOException e) {
			logger.warn("Error copying release file to " + sctDataFolder + ". Nested exception is : \n" + e.fillInStackTrace());
			return false;
			
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(out);
		}
		String rvfVersion = getRVFVersion(product, version);
		logger.info("RVF release version:" + rvfVersion);
		if (schemaNames.contains(rvfVersion) ) {
			logger.info("Release version is already known in RVF and the existing one will be deleted and reloaded: " + rvfVersion);
		}
		List<String> rf2FilesLoaded = new ArrayList<>();
		String schemaName = loadSnomedData(rvfVersion, rf2FilesLoaded, fileDestination);
		logger.info("schemaName = " + schemaName);
		schemaNames.add(schemaName);
		return true;
	}
	
	
	public boolean uploadReleaseDataIntoDB(final InputStream inputStream, String fileName, String schemaName) throws BusinessServiceException {
		// copy release pack zip to data location
		logger.info("Receiving release data - " + fileName);
		final File fileDestination = new File(sctDataFolder.getAbsolutePath(), fileName);
		OutputStream out = null;
		try {
			out = new FileOutputStream(fileDestination);
			IOUtils.copy(inputStream, out);
			logger.info("Release file copied to : " + fileDestination.getAbsolutePath());
		} catch (final IOException e) {
			logger.warn("Error copying release file to " + sctDataFolder + ". Nested exception is : \n" + e.fillInStackTrace());
			return false;
			
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(out);
		}
		
		if (schemaNames.contains(schemaName)) {
			logger.info("Release version is already known in RVF and the existing one will be deleted and reloaded: " + schemaName);
		}
		logger.info("Loading data into schema " + schemaName);
		List<String> rf2FilesLoaded = new ArrayList<>();
		loadSnomedData(schemaName, rf2FilesLoaded, fileDestination);
		logger.info("schemaName = " + schemaName);
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
		return loadSnomedData(versionName, false, rf2FilesLoaded, zipDataFile);
	}

	private String loadSnomedData(final String versionName, boolean isAppendToVersion, List<String> rf2FilesLoaded, final File... zipDataFile) throws BusinessServiceException {
		File outputFolder = null;
		final String createdSchemaName = versionName.startsWith(RVF_DB_PREFIX) ? versionName : RVF_DB_PREFIX + versionName;
		final long startTime = Calendar.getInstance().getTimeInMillis();
		try {
			outputFolder = new File(FileUtils.getTempDirectoryPath(), createdSchemaName);
			logger.info("Setting output folder location = " + outputFolder.getAbsolutePath());
			if (outputFolder.exists()) {
				logger.info("Output folder already exists and will be deleted before recreating.");
				outputFolder.delete();
			} 
			outputFolder.mkdir();
			// extract SNOMED CT content from zip file
			
			for (final File zipFile : zipDataFile) {
				ZipFileUtils.extractFilesFromZipToOneFolder(zipFile, outputFolder.getAbsolutePath());
			}
			if (!isAppendToVersion) {
				createSchema(createdSchemaName);
			}
			loadReleaseFilesToDB(outputFolder, rvfDynamicDataSource, rf2FilesLoaded, createdSchemaName);
		} catch (final SQLException | IOException e) {
			List<String> fileNames = Arrays.asList(zipDataFile).stream().map(file -> file.getName()).collect(Collectors.toList());
			final String errorMsg = String.format("Error while loading file %s into version %s", Arrays.toString(fileNames.toArray()), versionName);
			logger.error(errorMsg,e);
			throw new BusinessServiceException(errorMsg, e);
		}  finally {
			// remove output directory so it does not occupy space
			FileUtils.deleteQuietly(outputFolder);
		}
		logger.info("Finished loading of data in : " + ((Calendar.getInstance().getTimeInMillis() - startTime) / 1000) + " seconds.");
		return createdSchemaName;
	}

	private void loadReleaseFilesToDB(final File rf2TextFilesDir, final RvfDynamicDataSource dataSource, List<String> rf2FilesLoaded, String schemaName) throws SQLException, FileNotFoundException {
		if (rf2TextFilesDir != null) {
			final String[] rf2Files = rf2TextFilesDir.list( new FilenameFilter() {
				
				@Override
				public boolean accept(final File dir, final String name) {
					return name.endsWith(".txt") && (name.startsWith("der2") || name.startsWith("sct2"));
				}
			});
			final ReleaseFileDataLoader dataLoader = new ReleaseFileDataLoader(dataSource, schemaName, new MySqlDataTypeConverter());
			dataLoader.loadFilesIntoDB(rf2TextFilesDir.getAbsolutePath(), rf2Files, rf2FilesLoaded);
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
			final File [] zipFiles = sctDataFolder.listFiles( new FilenameFilter() {
				
				public boolean accept(final File dir, final String name) {
					final String[] tokens = name.split("_");
					final String lastToken = tokens[tokens.length -1];
					return lastToken.endsWith(ZIP_FILE_EXTENSION) && lastToken.contains(knownVersion);
				}
			});
			filesFound.addAll(Arrays.asList(zipFiles));
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
			logger.error("Failed to create db schema and tables for version:" + combinedVersionName +" due to " + e.fillInStackTrace());
		}
		//select data from known version schema and insert into the new schema
		for (String knownSchema : knownVersions) {
			if (isKnownRelease(knownSchema)) {
				isFailed = true;
				logger.error("Known schema doesn't exist for:" + knownSchema);
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
		logger.info("Time taken to combine both known versions into one schema in seconds: " + (endTime-startTime)/1000);
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
		final String disableIndex = "ALTER TABLE " + tableName + " DISABLE KEYS";
		final String enableIndex = "ALTER TABLE " + tableName + " ENABLE KEYS";
		String selectDataFromASql = "select a.* from " + sourceSchemaA + "." + tableName + " a where not exists ( select c.id from " + sourceSchemaB + "." 
				+ tableName + " c where a.id=c.id)";
		String latestDataFromASelectSql = "select a.* from " + sourceSchemaA + "." + tableName + " a where exists ( select b.id from " + sourceSchemaB + "." + tableName 
					+ " b where a.id=b.id and cast(a.effectivetime as datetime) >= cast(b.effectivetime as datetime))";
		
		String selectDataFromBSql = "select a.* from " + sourceSchemaB + "." + tableName + " a where not exists ( select c.id from " + sourceSchemaA + "." 
				+ tableName + " c where a.id=c.id)";
		String latestDataFromBSelectSql = "select a.* from " + sourceSchemaB + "." + tableName + " a where exists ( select b.id from " + sourceSchemaA + "." + tableName 
					+ " b where a.id=b.id and cast(a.effectivetime as datetime) > cast(b.effectivetime as datetime))";
		
		final String insertSql = "insert into " + targetSchema + "." + tableName  + " ";
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
			String msg = "Failed to insert data to table: " + tableName;
			logger.error(msg + " due to " + e.fillInStackTrace());
			throw new BusinessServiceException(msg, e);
		}
	}
	
	
	
	
	private void copyData(String tableName, String sourceSchema, String targetSchema) throws BusinessServiceException {
		final String disableIndex = "ALTER TABLE " + tableName + " DISABLE KEYS";
		final String enableIndex = "ALTER TABLE " + tableName + " ENABLE KEYS";
		final String sql = "insert into " + targetSchema + "." + tableName  + " select * from " + sourceSchema + "." + tableName;
		logger.debug("Copying table {} with sql {} ", tableName, sql);
		try (Connection connection = rvfDynamicDataSource.getConnection(targetSchema);
			Statement statement = connection.createStatement() ) {
			statement.execute(disableIndex);
			statement.execute(sql);
			statement.execute(enableIndex);
		} catch (final SQLException e) {
			String msg = "Failed to insert data to table: " + tableName;
			logger.error(msg + " due to " + e.fillInStackTrace());
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
			PreparedStatement statement = connection.prepareStatement(sql);) {
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
			logger.error("Failed to select table name from db schema: " + schemaName +" due to " + e.fillInStackTrace());
		}
		return result;
	}

	public void copyTableData(String sourceVersion, String destinationVersion, 
			String tableNamePattern, List<String> excludeTableNames) throws BusinessServiceException {
		final long startTime = System.currentTimeMillis();
		StringBuilder errorMsg = new StringBuilder();;
		if (!isKnownRelease(sourceVersion)) {
			errorMsg.append(VERSION_NOT_FOUND + sourceVersion); 
		}
		if (!isKnownRelease(destinationVersion)) {
			errorMsg.append(VERSION_NOT_FOUND + destinationVersion); 
		}
		if (errorMsg.length() > 0) {
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
			final String deleteSql = "delete a.* from " + schema + "." + snapshotTbl + " a where exists ( select b.id from " + schema + "." + deltaTbl + " b where a.id=b.id)";
			logger.debug("Delete data from snapshot table sql:" + deleteSql);
			final String insertSql = "insert into " + schema + "." + snapshotTbl  + " select * from " + schema + "." + deltaTbl;
			logger.debug("Insert delta into snapshot table sql:" + insertSql);
			try (Connection connection = rvfDynamicDataSource.getConnection(schema);
					Statement statement = connection.createStatement() ) {
				statement.execute(deleteSql);
				statement.execute(insertSql);
			} catch (final SQLException e) {
				logger.error("Failed to update table {} with data from {}  due to {} ", snapshotTbl, deltaTbl, e.fillInStackTrace());
			}
		}
		
	}

	
	public String loadSnomedDataIntoExistingDb(String productVersion, List<String> rf2FilesLoaded, File... zipDataFile) throws BusinessServiceException {
		return loadSnomedData(productVersion, true, rf2FilesLoaded, zipDataFile);
	}

	
	public void copyTableData(String sourceSchemaA, String sourceSchemaB, String destinationSchema, String tableNamePattern,
			List<String> excludeTableNames) throws BusinessServiceException {
		final long startTime = System.currentTimeMillis();
		StringBuilder errorMsg = new StringBuilder();
		if (!isKnownRelease(sourceSchemaA)) {
			errorMsg.append(VERSION_NOT_FOUND + sourceSchemaA); 
		}
		if (!isKnownRelease(sourceSchemaB)) {
			errorMsg.append(VERSION_NOT_FOUND + sourceSchemaB); 
		}
		if (!isKnownRelease(destinationSchema)) {
			errorMsg.append(VERSION_NOT_FOUND + destinationSchema); 
		}
		if (errorMsg.length() > 0) {
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
		logger.info("Creating db schema " + schemaName);
		//clean and create database
		String dropStr = "drop database if exists " + schemaName + ";";
		String createDbStr = "create database if not exists "+ schemaName + ";";
		try (Statement statement = dataSource.getConnection().createStatement()) {
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
		logger.info(schemaName + " is created successfully.");
		return schemaName;
	}

	public void clearQAResult(Long runId) {
		String deleteQaResultSQL = " delete from rvf_master.qa_result where run_id = " + runId;
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement() ) {
			statement.execute(deleteQaResultSQL);
		} catch (final SQLException e) {
			logger.error("Failed to delete data from qa_result table for runId {}  due to {} ", runId, e.fillInStackTrace());
		}
	}
	
	public String generateBinaryArchive(String schemaName) throws BusinessServiceException {
		if (schemaName == null || !isKnownRelease(schemaName)) {
			throw new IllegalArgumentException("No schema found for " + schemaName);
		}
		File dataDir = new File(mysqlDataDir);
		File binaryFile = new File(mysqlDataDir, schemaName);
		if (!dataDir.canRead()) {
			logger.error("Can't access directory " + dataDir.getPath());
			try {
				GroupPrincipal group = Files.readAttributes(dataDir.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
				UserPrincipal owner = Files.readAttributes(dataDir.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).owner();
				logger.info("user group:" + group.toString());
				logger.info("owner :" + owner.toString());
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
			logger.info("Mysql binary archive file is created " + archiveFile.getPath());
			ResourceManager resourceManager = new ResourceManager(mysqlBinaryStorageConfig, cloudResourceLoader);
			resourceManager.writeResource(archiveFile.getName(), new FileInputStream(archiveFile));
			logger.info("Mysql binary archive file " + archiveFile.getName() + " is loaded to " + mysqlBinaryStorageConfig.toString());
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to zip binary file " + binaryFile.getAbsolutePath(), e);
		}
		return archiveFile.getName();
	}

	public boolean restoreReleaseFromBinaryArchive(String archiveFileName) throws IOException {
		ResourceManager resourceManager = new ResourceManager(mysqlBinaryStorageConfig, cloudResourceLoader);
		InputStream inputStream = resourceManager.readResourceStreamOrNullIfNotExists(archiveFileName);
		if (inputStream == null) {
			logger.info("No resource available for " + archiveFileName + " via " + mysqlBinaryStorageConfig.toString());
			return false;
		}
		File outputFile = downloadFile(inputStream, archiveFileName);
		if (outputFile == null) {
			logger.error("Failed to download " + archiveFileName + " via " + mysqlBinaryStorageConfig.toString());
			return false;
		}
		File outputDir = new File(mysqlDataDir, archiveFileName.replace(".zip", ""));
		if (outputDir.exists()) {
			outputDir.delete();
		}
		outputDir.mkdir();
		org.ihtsdo.otf.utils.ZipFileUtils.extractFilesFromZipToOneFolder(outputFile, outputDir.getAbsolutePath());
		logger.info("Mysql binary files are restored successfully in " +  outputDir.getPath());
		fetchRvfSchemasFromDb();
		return true;
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
		InputStream inputStream = null;
		try {
			ResourceManager resourceManager = new ResourceManager(releaseStorageConfig, cloudResourceLoader);
			inputStream = resourceManager.readResourceStream(releaseFilename);
		} catch (IOException e) {
			logger.error("Error while reading release package " + releaseFilename + " due to " + e.fillInStackTrace());
			throw new BusinessServiceException("Failed to read file " + releaseFilename + " via " + releaseStorageConfig.toString(), e);
		}
		uploadReleaseDataIntoDB(inputStream, releaseFilename, schemaName);
		return true;
	}

	private File downloadFile(InputStream input, String outputFilename) {
		final File fileDestination = new File(sctDataFolder.getAbsolutePath(), outputFilename);
		OutputStream out = null;
		try {
			out = new FileOutputStream(fileDestination);
			IOUtils.copy(input, out);
			logger.info("Release file copied to : " + fileDestination.getAbsolutePath());
			return fileDestination;
		} catch (final IOException e) {
			logger.warn("Error copying release file to " + sctDataFolder + ". Nested exception is : \n" + e.fillInStackTrace());
			return null;
		} finally {
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(out);
		}
	}

	public String getEditionAndVersion(final File zipDataFile)  throws BusinessServiceException {
		String editionAndVersion = "";
		String snomedFile = "";
		List<String> zipFileList = getFileList(zipDataFile);
		Optional<String> sctOrDerFile = zipFileList.stream()
							.filter(file -> ( file.indexOf("sct_") != -1 
										|| file.indexOf("der2_") != -1)
										&& file.endsWith(".txt"))
							.findFirst();
		if (!sctOrDerFile.isPresent()) {
			throw new BusinessServiceException("There are no RF2 files in data file: " + zipDataFile);
		}
		snomedFile = sctOrDerFile.get();
		Matcher matcher = Pattern.compile(".*_([0-9]+)\\.txt").matcher(snomedFile);
		if (matcher.find()) {
			editionAndVersion = 
				mapFilenameToEdition(snomedFile).toLowerCase() + "_"
				+ matcher.group(1);
		} else {
			throw new BusinessServiceException(
				"Could not find RF2 file with standard name in data zip file " 
				+ zipDataFile.getName());
		}
		logger.info ("Identified edition and version " + editionAndVersion + " from zip file " + zipDataFile.getName());
		return editionAndVersion;
	}

	private String mapFilenameToEdition(String name) {
		String edition = "INT";
		Map<String,String> fileNameToEditionMap = new HashMap<String,String>();
		fileNameToEditionMap.put("SpanishExtension.*_INT", "ES");
		fileNameToEditionMap.put("_NL_[0-9]+\\.txt", "NL");
		fileNameToEditionMap.put("_AU1000036_[0-9]+\\.txt", "AU");
		fileNameToEditionMap.put("_NZ1000210_[0-9]+\\.txt", "NZ");
		fileNameToEditionMap.put("_US1000124_[0-9]+\\.txt", "US");
		fileNameToEditionMap.put("_BE1000172_[0-9]+\\.txt", "BE");
		fileNameToEditionMap.put("_SE1000052_[0-9]+\\.txt", "SE");
		fileNameToEditionMap.put("GB1000000_[0-9]+\\.txt", "UK");
		fileNameToEditionMap.put("_INT_[0-9]+\\.txt", "INT");
		for (String pattern : fileNameToEditionMap.keySet()) {
			Matcher editionMatcher = Pattern.compile(pattern).matcher(name);
			if (editionMatcher.find()) {
				edition = fileNameToEditionMap.get(pattern);
				break;
			}

		}
		return edition;
	}

	private List<String> getFileList(final File dataFile) throws BusinessServiceException {
		try {
			List<String> fileList = ZipFileUtils.listFiles(dataFile);
			return fileList;
		} catch (IOException e) {
			throw new BusinessServiceException("Could not get file list from " + dataFile, e);
		}
	}
}
