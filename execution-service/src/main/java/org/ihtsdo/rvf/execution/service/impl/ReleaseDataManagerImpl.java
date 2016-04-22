package org.ihtsdo.rvf.execution.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.util.RvfDynamicDataSource;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An implementation of a {@link org.ihtsdo.rvf.execution.service.ReleaseDataManager}. The method afterPropertiesSet must always be called
 * immediately after instantiating this class outside of Spring context. This class uses a 'data folder' as the place to
 * store all known releases.
 */
@Service
public class ReleaseDataManagerImpl implements ReleaseDataManager, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(ReleaseDataManagerImpl.class);
	private static final String RVF_DB_PREFIX = "rvf_";
	private String sctDataLocation;
	private File sctDataFolder;
	@Resource(name = "snomedDataSource")
	private BasicDataSource snomedDataSource;
	@Autowired
	private RvfDynamicDataSource rvfDynamicDataSource;
	private final Map<String, String> releaseSchemaNameLookup = new HashMap<>();
	/**
	 * No args constructor for IOC. Always call 'init' method after creation
	 */
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
		try ( ResultSet catalogs = snomedDataSource.getConnection().getMetaData().getCatalogs()) {
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
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(fileDestination);
			IOUtils.copy(inputStream, output);
			logger.info("Release file copied to : " + fileDestination.getAbsolutePath());
		} catch (final IOException e) {
			logger.warn("Error copying release file to " + sctDataFolder + ". Nested exception is : \n" + e.fillInStackTrace());
			return false;
			
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(output);
		}
		// if we are here then release date is a valid date format
		// now call loadSnomedData method passing release zip, if there is no matching database
		String productVersion = product + "_" + version;
		logger.info("Product version:" + productVersion);
		if (releaseSchemaNameLookup.keySet().contains(productVersion) ) {
			logger.info("Product version is already known in RVF and the existing one will be deleted and reloaded: " + productVersion);
		}
		logger.info("Loading data into schema " + RVF_DB_PREFIX + productVersion);
		List<String> rf2FilesLoaded = new ArrayList<>();
		final String schemaName = loadSnomedData(productVersion, rf2FilesLoaded, fileDestination);
		logger.info("schemaName = " + schemaName);
		// now add to releaseSchemaNameLookup
		releaseSchemaNameLookup.put(productVersion, schemaName);
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
		final String createdSchemaName = RVF_DB_PREFIX + versionName;
		final long startTime = Calendar.getInstance().getTimeInMillis();
		try {
			outputFolder = new File(FileUtils.getTempDirectoryPath(), "rvf_loader_data_" + versionName);
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
				try (Connection connection = snomedDataSource.getConnection()) {
					connection.setAutoCommit(true);
					createDBAndTables(createdSchemaName, connection);
				}
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

	private void createDBAndTables(final String schemaName, final Connection connection) throws SQLException, IOException {
		//clean and create database
		String dropStr = "drop database if exists " + schemaName + ";";
		String createDbStr = "create database if not exists "+ schemaName + ";";
		String  useStr = "use " + schemaName + ";";
		try(Statement statement = connection.createStatement()) {
			statement.execute(dropStr);
			statement.execute(createDbStr);
			statement.execute(useStr);
		}
		try (InputStream input = getClass().getResourceAsStream("/sql/create-tables-mysql.sql")) {
			final ScriptRunner runner = new ScriptRunner(connection);
			runner.runScript(new InputStreamReader(input));
		}
	}

	private void loadReleaseFilesToDB(final File rf2TextFilesDir, final RvfDynamicDataSource dataSource, List<String> rf2FilesLoaded, String schemaName) throws SQLException, FileNotFoundException {
		if (rf2TextFilesDir != null) {
			final String[] rf2Files = rf2TextFilesDir.list( new FilenameFilter() {
				
				@Override
				public boolean accept(final File dir, final String name) {
					if ( name.endsWith(".txt") && (name.startsWith("der2") || name.startsWith("sct2")))
					{
						return true;
					}
					return false;
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
		return releaseSchemaNameLookup.containsKey(releaseVersion);
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

	public void setSnomedDataSource(final BasicDataSource snomedDataSourceX) {
		snomedDataSource = snomedDataSourceX;
	}

	/**
	 * Returns the schema name that corresponds to  the given release.
	 * @param releaseVersion the product name and release date as a yyyymmdd string (e.g. int_20140731)
	 * @return the corresponding schema name
	 */
	@Override
	public String getSchemaForRelease(final String releaseVersion) {
		return releaseSchemaNameLookup.get(releaseVersion);
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
					if (lastToken.endsWith(".zip") && lastToken.contains(knownVersion)) {
						return true;
					}
					return false;
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
		try (Connection connection = snomedDataSource.getConnection()) {
			createDBAndTables(schemaName, connection);
			releaseSchemaNameLookup.put(combinedVersionName, schemaName);
		} catch (SQLException | IOException e) {
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
				
				isFailed = copyTable(tableName, knownSchema, schemaName);
				if (isFailed) {
					break;
				}
			}
		}
		final long endTime = System.currentTimeMillis();
		logger.info("Time taken to combine both known versions into one schema in seconds: " + (endTime-startTime)/1000);
		return !isFailed;
	}
	
	
	private boolean copyTable(String tableName, String sourceSchema, String targetSchema) {
		boolean isFailed = false;
		final String disableIndex = "ALTER TABLE " + tableName + " DISABLE KEYS";
		final String enableIndex = "ALTER TABLE " + tableName + " ENABLE KEYS";
		final String sql = "insert into " + targetSchema + "." + tableName  + " select * from " + sourceSchema + "." + tableName;
		logger.debug("Copying table {}", tableName);
		try (Connection connection = snomedDataSource.getConnection();
				Statement statement = connection.createStatement() ) {
			statement.execute(disableIndex);
			statement.execute(sql);
			statement.execute(enableIndex);
		} catch (final SQLException e) {
			isFailed = true;
			logger.error("Failed to insert data to table: " + tableName +" due to " + e.fillInStackTrace());
		}
		return isFailed;
	}
	
	
	private List<String> getValidTableNamesFromSchema(String schemaName, String tableNamePattern) {
		List<String> result = new ArrayList<>();
		Collection<String> mappedTables = RF2FileTableMapper.getAllTableNames();
		try (Connection connection = snomedDataSource.getConnection();
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
		//just remove the schema name from the map for the time being as the actual schema data is
		//removed a RvfDbScheduleEventGenerator
		if (version != null) {
			releaseSchemaNameLookup.remove(version);
		}
	}

	@Override
	public void copyTableData(String sourceVersion, String destinationVersion, String tableNamePattern) {
		final long startTime = System.currentTimeMillis();
		String sourceSchema = releaseSchemaNameLookup.get(sourceVersion);
		String destinationSchema = releaseSchemaNameLookup.get(destinationVersion);
		for (final String tableName : getValidTableNamesFromSchema(sourceSchema, tableNamePattern)) {
			if (copyTable(tableName, sourceSchema, destinationSchema)) {
				break;
			}
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
			try (Connection connection = snomedDataSource.getConnection();
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
}
