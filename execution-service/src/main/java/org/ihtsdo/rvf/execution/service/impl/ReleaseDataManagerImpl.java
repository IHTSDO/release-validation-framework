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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * An implementation of a {@link org.ihtsdo.rvf.execution.service.ReleaseDataManager}. The method afterPropertiesSet must always be called
 * immediately after instantiating this class outside of Spring context. This class uses a 'data folder' as the place to
 * store all known releases.
 */
@Service
public class ReleaseDataManagerImpl implements ReleaseDataManager, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(ReleaseDataManagerImpl.class);
	private static final String RVF_DB_PREFIX = "rvf_int_";
	private String sctDataLocation;
	private File sctDataFolder;
	@Resource(name = "snomedDataSource")
	private BasicDataSource snomedDataSource;
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
		//TODO remove this so that it doesn't load release data during start up 
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
	 * @param inputStream the release as an input stream
	 * @param overWriteExisting if existing file has to over written
	 * @param purgeExistingDatabase if existing database must be recreated
	 * @return result of the copy operation - false if there are errors.
	 */
	@Override
	public boolean uploadPublishedReleaseData(final InputStream inputStream, final String fileName, final String version) {
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
		if (releaseSchemaNameLookup.keySet().contains(version) ) {
			logger.info("Version is already known in RVF and the existing one will be deleted and reloaded: " + version);
		}
		logger.info("Loading data into schema " + RVF_DB_PREFIX + version);
		final String schemaName = loadSnomedData(version, fileDestination);
		logger.info("schemaName = " + schemaName);
		// now add to releaseSchemaNameLookup
		releaseSchemaNameLookup.put(version, schemaName);
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
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Override
	//TODO seems that this is only used in test
	public boolean uploadPublishedReleaseData(final File releasePackZip, final String version) {
		boolean result = false;
		try(InputStream inputStream = new FileInputStream(releasePackZip)) {
			 result = uploadPublishedReleaseData(inputStream, releasePackZip.getName(), version);
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
	 */
	@Override
	public String loadSnomedData(final String versionName, final File... zipDataFile) {
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
			try (Connection connection = snomedDataSource.getConnection()) {
				connection.setAutoCommit(true);
				createDBAndTables(createdSchemaName, connection);
				loadReleaseFilesToDB(outputFolder, connection);
			}
//			loadDataViaScript(versionName, outputFolder);
			// add schema name to look up map
			releaseSchemaNameLookup.put(versionName, createdSchemaName);
		} catch (final SQLException | IOException e) {
			logger.error("Error while loading file {} into version {} due to {}", zipDataFile, versionName, e.fillInStackTrace());
			return null;
		}  finally {
			// remove output directory so it does not occupy space
			FileUtils.deleteQuietly(outputFolder);
		}
		logger.info("Finished loading of data in : " + ((Calendar.getInstance().getTimeInMillis() - startTime) / 60000) + " minutes.");
		return createdSchemaName;
	}

	private void createDBAndTables(final String schemaName, final Connection connection) throws SQLException, IOException {
		//clean and create database
		final String dropStr = "drop database if exists " + schemaName + ";";
		final String createDbStr = "create database if not exists "+ schemaName + ";";
		final String  useStr = "use " + schemaName + ";";
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

	private void loadReleaseFilesToDB(final File rf2TextFilesDir, final Connection connection) throws SQLException, FileNotFoundException {
		
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
			final ReleaseFileDataLoader dataLoader = new ReleaseFileDataLoader(connection, new MySqlDataTypeConverter());
			dataLoader.loadFilesIntoDB(rf2TextFilesDir.getAbsolutePath(), rf2Files);
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
	 * @param releaseVersion the release date as a yyyymmdd string (e.g. 20140731)
	 * @return the corresponding schema name
	 */
	@Override
	public String getSchemaForRelease(final String releaseVersion) {
		return releaseSchemaNameLookup.get(releaseVersion);
	}

	/**
	 * Sets the schema name that corresponds to  the given release.
	 * @param releaseVersion the release date as a yyyymmdd string (e.g. 20140731)
	 * @param schemaName the corresponding schema name
	 */
	@Override
	public void setSchemaForRelease(final String releaseVersion, final String schemaName) {
		releaseSchemaNameLookup.put(releaseVersion, schemaName);
	}

	@Override
	public File getZipFileForKnownRelease(final String knownVersion) {
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
			
			if( zipFiles != null && zipFiles.length > 0) {
				if (zipFiles.length > 1) {
					logger.warn("Found more than one zip files having version:" + knownVersion);
				}
				return zipFiles[0];
			} else {
				logger.warn("Failed to find zip file for {} in directory {}", knownVersion, sctDataFolder);
			}
		}
		return null;
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
		} catch (SQLException | IOException e) {
			isFailed = true;
			logger.error("Failed to create db schema and tables for version:" + combinedVersionName +" due to " + e.fillInStackTrace());
		}
		//select data from known version schema and insert into the new schema
		for (final String known : knownVersions) {
			final String knownSchema = releaseSchemaNameLookup.get(known);
			logger.info("Adding known version {} in schema {}", known, knownSchema);
			for (final String tableName : RF2FileTableMapper.getAllTableNames()) {
				final String disableIndex = "ALTER TABLE " + tableName + " DISABLE KEYS";
				final String enableIndex = "ALTER TABLE " + tableName + " ENABLE KEYS";
				final String sql = "insert into " + schemaName + "." + tableName  + " select * from " + knownSchema + "." + tableName;
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
			}
		}
		final long endTime = System.currentTimeMillis();
		logger.info("Time taken to combine both known versions into one schema in seconds: " + (endTime-startTime)/1000);
		if (!isFailed) {
			releaseSchemaNameLookup.put(combinedVersionName, schemaName);
		}
		return !isFailed;
	}
}
