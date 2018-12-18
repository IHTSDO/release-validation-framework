package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ValidationReleaseStorageConfig;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.ValidationMysqlBinaryStorageConfig;
import org.ihtsdo.rvf.execution.service.util.RvfDynamicDataSource;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * An implementation of a {@link org.ihtsdo.rvf.execution.service.ReleaseDataManager}. The method afterPropertiesSet must always be called
 * immediately after instantiating this class outside of Spring context. This class uses a 'data folder' as the place to
 * store all known releases.
 */
@Service
public class ReleaseDataManagerImpl implements ReleaseDataManager, InitializingBean {

	private static final String VERSION_NOT_FOUND = "Version not found in RVF database ";
	private static final String ZIP_FILE_EXTENSION = ".zip";
	private static final Logger logger = LoggerFactory.getLogger(ReleaseDataManagerImpl.class);
	private static final String RVF_DB_PREFIX = "rvf_";
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
	
	private final Map<String, String> releaseSchemaNameLookup = new ConcurrentHashMap<>();
	
	@Autowired
	private ResourceLoader cloudResourceLoader;

	@Autowired
	private ValidationReleaseStorageConfig releaseStorageConfig;
	
	@Autowired
	private ValidationMysqlBinaryStorageConfig mysqlBinaryStorageConfig;
	
	public ReleaseDataManagerImpl() {
	}

	/**
	 * The init method that sets up the data folder where all releases are stored. This method must always be called
	 * immediately after instantiating this class outside of Spring context. If there are a large number of releases
	 * that exist in the data folder that have never been processed, then this will take a while
	 * //todo move to an async process at some time!
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Sct Data Location passed = " + sctDataLocation);
		if (sctDataLocation == null || sctDataLocation.length() == 0) {
			sctDataLocation = FileUtils.getTempDirectoryPath() + System.getProperty("file.separator") + "rvf-sct-data";
		}
		sctDataFolder = new File(sctDataLocation);
		if (!sctDataFolder.exists()) {
			if (sctDataFolder.mkdirs()) {
				logger.info("Created data folder at : " + sctDataLocation);
			} else {
				logger.error("Unable to create data folder at path : " + sctDataLocation);
				throw new IllegalArgumentException("Bailing out because data location can not be set to : " + sctDataLocation);
			}
		}
		logger.info("Using data location as :" + sctDataFolder.getAbsolutePath());
		// now populate releaseLookup map with existing releases - but ask not to purge existing databases
		populateLookupMap();
	}

	/**
	 * Utility method that generates a map of all known releases based on the contents of the data folder.
	 */
	protected void populateLookupMap() {
		// now get list of existing RVF_INT like databases
		try ( ResultSet catalogs = rvfDynamicDataSource.getConnection(masterSchema).getMetaData().getCatalogs()) {
			while (catalogs.next()) {
				final String schemaName = catalogs.getString(1);
				if (schemaName.startsWith(RVF_DB_PREFIX)) {
					final String version = schemaName.substring(RVF_DB_PREFIX.length());
						releaseSchemaNameLookup.put(version, schemaName);
				}
			}
		} catch (final SQLException e) {
			logger.error("Error getting list of existing schemas. Nested exception is : \n" + e.fillInStackTrace());
		}

	}
	
	public String getRVFVersion(String product, String releaseVersion) {
		return product + "_" + releaseVersion;
	}

	/**
	 * Method that uses a {@link java.io.InputStream}  to copy a known/published release pack into the data folder.
	 * This method is not intended to be used
	 * for uploading prospective releases since they do not need to be stored for later use.
	 *
	 */
	@Override
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
		// if we are here then release date is a valid date format
		// now call loadSnomedData method passing release zip, if there is no matching database
		String rvfVersion = getRVFVersion(product, version);
		logger.info("RVF release version:" + rvfVersion);
		if (releaseSchemaNameLookup.keySet().contains(rvfVersion) ) {
			logger.info("Release version is already known in RVF and the existing one will be deleted and reloaded: " + rvfVersion);
		}
		logger.info("Loading data into schema " + RVF_DB_PREFIX + rvfVersion);
		List<String> rf2FilesLoaded = new ArrayList<>();
		final String schemaName = loadSnomedData(rvfVersion, rf2FilesLoaded, fileDestination);
		logger.info("schemaName = " + schemaName);
		// now add to releaseSchemaNameLookup
		releaseSchemaNameLookup.put(rvfVersion, schemaName);
		return true;
	}
	
	
	@Override
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
		
		if (releaseSchemaNameLookup.keySet().contains(schemaName)) {
			logger.info("Release version is already known in RVF and the existing one will be deleted and reloaded: " + schemaName);
		}
		logger.info("Loading data into schema " + schemaName);
		List<String> rf2FilesLoaded = new ArrayList<>();
		loadSnomedData(schemaName, rf2FilesLoaded, fileDestination);
		logger.info("schemaName = " + schemaName);
		// now add to releaseSchemaNameLookup
		releaseSchemaNameLookup.put(schemaName, schemaName);
		return true;
	}

	/**
	 * Method that copies a known/published release pack into the data folder. This method is not intended to be used
	 * for uploading prospective releases since they do not need to be stored for later use.
	 *
	 * @param releasePackZip the release as a zip file
	 * @param overWriteExisting if existing file has to over written
	 * @param purgeExistingDatabase if existing database must be recreated
	 * @return result of the copy operation - false if there are errors.
	 * @throws BusinessServiceException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Override
	public boolean uploadPublishedReleaseData(final File releasePackZip, final String product, final String version) throws BusinessServiceException {
		boolean result = false;
		try(InputStream inputStream = new FileInputStream(releasePackZip)) {
			 result = uploadPublishedReleaseData(inputStream, releasePackZip.getName(), product, version);
		} catch (final IOException e) {
			logger.error("Error during upload release:" + releasePackZip.getName(), e);
		}
		return result;
	}
	
	
	
	/**
	 * Method that loads given SNOMED CT data into a RF2 compliant database. For published release, use the
	 * uploadPublishedReleaseData method since it stores the data and makes better reuse of existing databases. Use
	 * this method directly if prospective build data is to be loaded.
	 *
	 * @param versionName the version of the data as a yyyymmdd string (e.g. 20140731)
	 * @param isAppend boolean that controls if existing database needs to be appended
	 * @param zipDataFile the zip file that contains the release data
	 * @return the name of the schema to which the data has been loaded.
	 * @throws BusinessServiceException 
	 */
	@Override
	public String loadSnomedData(final String versionName, List<String> rf2FilesLoaded, final File... zipDataFile) throws BusinessServiceException {
		return loadSnomedData(versionName, false, rf2FilesLoaded, zipDataFile);
	}

	private String loadSnomedData(final String versionName, boolean isAppendToVersion, List<String> rf2FilesLoaded, final File... zipDataFile) throws BusinessServiceException {
		File outputFolder = null;
		final String createdSchemaName = versionName.startsWith("rvf_") ? versionName : RVF_DB_PREFIX + versionName;
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
			
			loadReleaseFilesToDB(outputFolder,rvfDynamicDataSource,rf2FilesLoaded, createdSchemaName);
			
			// add schema name to look up map
			releaseSchemaNameLookup.put(versionName, createdSchemaName);
		} catch (final SQLException | IOException e) {
			final String errorMsg = String.format("Error while loading file %s into version %s", zipDataFile, versionName);
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


	/**
	 * Verifies if the given releaseVersion has already been loaded into a database.
	 *
	 * @param releaseVersion the release date as a yyyymmdd string (e.g. 20140731)
	 * @return if a schema for the version already exists
	 */
	@Override
	public boolean isKnownRelease(final String releaseVersion) {
		if (!releaseVersion.startsWith(RVF_DB_PREFIX)) {
			return releaseSchemaNameLookup.containsKey(releaseVersion);
		}
		return releaseSchemaNameLookup.values().contains(releaseVersion);
		
	}

	/**
	 * Returns a list of all known releases that have been uploaded into the database
	 * @return set of all releases
	 */
	@Override
	public Set<String> getAllKnownReleases() {
		return releaseSchemaNameLookup.keySet();
	}

	public void setSctDataLocation(final String sctDataLocationX) {
		sctDataLocation = sctDataLocationX;
	}

	/**
	 * Returns the schema name that corresponds to  the given release.
	 * @param releaseVersion the product name and release date as a yyyymmdd string (e.g. int_20140731)
	 * @return the corresponding schema name
	 */
	@Override
	public String getSchemaForRelease(final String releaseVersion) {
		if (releaseVersion != null) {
			if (!releaseVersion.startsWith(RVF_DB_PREFIX)) {
				return releaseSchemaNameLookup.get(releaseVersion);
			}
			return releaseVersion;
		}
		return null;
	}

	/**
	 * Sets the schema name that corresponds to  the given release.
	 * @param releaseVersion the product name and release date as a yyyymmdd string (e.g. int_20140731)
	 * @param schemaName the corresponding schema name
	 */
	@Override
	public void setSchemaForRelease(final String releaseVersion, final String schemaName) {
		releaseSchemaNameLookup.put(releaseVersion, schemaName);
	}

	@Override
	public List<File> getZipFileForKnownRelease(final String knownVersion) {
		List<File> filesFound = new ArrayList<>();
		if (knownVersion != null ) {
			final File [] zipFiles = sctDataFolder.listFiles( new FilenameFilter() {
				@Override
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
	
	
	@Override
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
		for (final String known : knownVersions) {
			final String knownSchema = releaseSchemaNameLookup.get(known);
			if (knownSchema == null) {
				isFailed = true;
				logger.error("Known schema doesn't exist for:" + known);
				break;
			}
			logger.info("Adding known version {} to schema {}", known, combinedVersionName);
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
		try (Connection connection = rvfDynamicDataSource.getConnection(schemaName);
				Statement statement = connection.createStatement() ) {
			String sql = "select table_name from INFORMATION_SCHEMA.TABLES WHERE table_schema ='" + schemaName + "'";
			if ( tableNamePattern != null) {
				sql = sql + " and table_name like '" + tableNamePattern + "'";
			}
			ResultSet resultSet = statement.executeQuery(sql);
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

	@Override
	public void dropVersion(String version) {
//		if (version != null && releaseSchemaNameLookup.containsKey(version)) {
//			String schema = releaseSchemaNameLookup.get(version);
//			try (Connection connection = rvfDynamicDataSource.getConnection(schema)) {
//				String dropSchemaSQL = "drop database "  + schema;
//				try( PreparedStatement statement = connection.prepareStatement(dropSchemaSQL)) {
//					statement.execute();
//				}
//			} catch(SQLException e) {
//				logger.error("Failed to drop schema " + schema);
//			}
//			releaseSchemaNameLookup.remove(version);
//		}
	}

	@Override
	public void copyTableData(String sourceVersion, String destinationVersion, String tableNamePattern, List<String> excludeTableNames) throws BusinessServiceException {
		final long startTime = System.currentTimeMillis();
		String sourceSchema = releaseSchemaNameLookup.get(sourceVersion);
		String destinationSchema = releaseSchemaNameLookup.get(destinationVersion);
		if (sourceSchema == null || destinationSchema == null) {
			StringBuilder errorMsg = new StringBuilder();
			if (sourceSchema == null) {
				errorMsg.append(VERSION_NOT_FOUND + sourceVersion); 
			}
			if (destinationSchema == null) {
				errorMsg.append(VERSION_NOT_FOUND + destinationVersion); 
			}
			throw new BusinessServiceException(errorMsg.toString());
		}
		for (final String tableName : getValidTableNamesFromSchema(sourceSchema, tableNamePattern)) {
			if (excludeTableNames != null && excludeTableNames.contains(tableName)) {
				continue;
			}
			copyData(tableName, sourceSchema, destinationSchema);
		}
		final long endTime = System.currentTimeMillis();
		logger.info("Copy data with table name like {} from {} into {} completed in seconds {} ", tableNamePattern, sourceSchema, destinationSchema, (endTime-startTime)/1000);
	}

	@Override
	public void updateSnapshotTableWithDataFromDelta(String prospectiveVersion) {
		String schema = releaseSchemaNameLookup.get(prospectiveVersion);
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

	@Override
	public String loadSnomedDataIntoExistingDb(String productVersion, List<String> rf2FilesLoaded, File... zipDataFile) throws BusinessServiceException {
		return loadSnomedData(productVersion, true, rf2FilesLoaded, zipDataFile);
	}

	@Override
	public void copyTableData(String sourceVersionA, String sourceVersionB, String destinationVersion, String tableNamePattern,
			List<String> excludeTableNames) throws BusinessServiceException {
		final long startTime = System.currentTimeMillis();
		String sourceSchemaA = releaseSchemaNameLookup.get(sourceVersionA);
		String sourceSchemaB = releaseSchemaNameLookup.get(sourceVersionB);
		String destinationSchema = releaseSchemaNameLookup.get(destinationVersion);
		if (sourceSchemaA == null || destinationSchema == null || sourceSchemaB ==null) {
			StringBuilder errorMsg = new StringBuilder();
			if (sourceSchemaA == null) {
				errorMsg.append(VERSION_NOT_FOUND + sourceVersionA); 
			}
			if (sourceSchemaA == null) {
				errorMsg.append(VERSION_NOT_FOUND + sourceVersionB); 
			}
			if (destinationSchema == null) {
				errorMsg.append(VERSION_NOT_FOUND + destinationVersion); 
			}
			throw new BusinessServiceException(errorMsg.toString());
		}
		for (final String tableName : getValidTableNamesFromSchema(sourceSchemaA, tableNamePattern)) {
			if (excludeTableNames != null && excludeTableNames.contains(tableName)) {
				continue;
			}
			copyData(tableName, sourceSchemaA, sourceSchemaB, destinationSchema);
		}
		final long endTime = System.currentTimeMillis();
		logger.info("Copy data with table name like {} from {} {} into {} completed in seconds {} ", tableNamePattern, sourceSchemaA, sourceSchemaB, destinationSchema, (endTime-startTime)/1000);
	}

	@Override
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
			try (InputStream input = getClass().getResourceAsStream("/sql/create-tables-mysql.sql")) {
				ScriptRunner runner = new ScriptRunner(rvfDynamicDataSource.getConnection(schemaName));
				runner.runScript(new InputStreamReader(input));
			} 
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to create tables for schema " + schemaName, e);
		}
		logger.info(schemaName + " is created successfully.");
		return schemaName;
	}

	@Override
	public void clearQAResult(Long runId) {
		String deleteQaResultSQL = " delete from rvf_master.qa_result where run_id = " + runId;
		try (Connection connection = rvfDynamicDataSource.getConnection(masterSchema);
				Statement statement = connection.createStatement() ) {
			statement.execute(deleteQaResultSQL);
		} catch (final SQLException e) {
			logger.error("Failed to delete data from qa_result table for runId {}  due to {} ", runId, e.fillInStackTrace());
		}
	}
	
	@Override
	public String generateBinaryArchive(String schemaName) throws BusinessServiceException {
		if (schemaName == null || !releaseSchemaNameLookup.values().contains(schemaName)) {
			throw new IllegalArgumentException("No schema found for " + schemaName);
		}
		File archiveFile = new File(FileUtils.getTempDirectoryPath(), schemaName + ZIP_FILE_EXTENSION);
		File binaryFile = new File(mysqlDataDir, schemaName);
		if (!binaryFile.exists()) {
			throw new BusinessServiceException("No mysql binary file found for " + binaryFile.getPath());
		}
		try {
			ZipFileUtils.zip(binaryFile.getAbsolutePath(), archiveFile.getAbsolutePath());
			logger.info("Mysql binary archive file is created " + archiveFile.getName());
			ResourceManager resourceManager = new ResourceManager(mysqlBinaryStorageConfig, cloudResourceLoader);
			resourceManager.writeResource(archiveFile.getName(), new FileInputStream(archiveFile));
			logger.info("Mysql binary archive file " + archiveFile.getName() + " is loaded to " + mysqlBinaryStorageConfig.toString());
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to zip binary file " + binaryFile.getAbsolutePath(), e);
		}
		return archiveFile.getName();
	}

	@Override
	public boolean restoreReleaseFromBinaryArchive(String archiveFileName) throws IOException {
		
		ResourceManager resourceManager = new ResourceManager(mysqlBinaryStorageConfig, cloudResourceLoader);
		InputStream inputStream = resourceManager.readResourceStreamOrNullIfNotExists(archiveFileName);
		if (inputStream == null) {
			return false;
		}
		
		File outputFile = downloadFile(inputStream, archiveFileName);
		if (outputFile == null) {
			return false;
		}
		File outputDir = new File(mysqlDataDir + archiveFileName.replace(".zip", ""));
		if (outputDir.exists()) {
			outputDir.delete();
		}
		outputDir.mkdir();
		org.ihtsdo.otf.utils.ZipFileUtils.extractFilesFromZipToOneFolder(outputFile, outputDir.getAbsolutePath());
		return true;
	}

	@Override
	public boolean uploadRelease(String releaseFilename, String schemaName)
			throws BusinessServiceException {
		
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
}
