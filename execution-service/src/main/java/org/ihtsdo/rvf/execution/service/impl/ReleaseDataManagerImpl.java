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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.ihtsdo.rvf.execution.service.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

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
		final Set<String> rvfDatabases = new HashSet<>();
		try {
			final ResultSet catalogs = snomedDataSource.getConnection().getMetaData().getCatalogs();
			while (catalogs.next()) {
				final String schemaName = catalogs.getString(1);
				if (schemaName.startsWith(RVF_DB_PREFIX)) {
					// get the last yyymmdd fragment which indicates release data
					final String releaseDate = schemaName.substring(RVF_DB_PREFIX.length());
					try {
						// we convert to data to verify we have a pattern matching yyyyMMdd
						final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
						sdf.setLenient(false);
						sdf.parse(releaseDate);
						// if we are here then release date is a valid date format
						logger.info("Registering existing schema as known release: " + schemaName);
						releaseSchemaNameLookup.put(releaseDate, schemaName);
					} catch (final ParseException e) {
						logger.warn("Error processing file. Not a valid release date : " + schemaName);
					}
					// also store for later processing
					rvfDatabases.add(schemaName);
				}
			}
			catalogs.close();
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
	public boolean uploadPublishedReleaseData(final InputStream inputStream, final String fileName, final String version, final boolean isAppend) {
		boolean result = false;
		// copy release pack zip to data location
		logger.info("Receiving release data - " + fileName);
		final File fileDestination = new File(sctDataFolder.getAbsolutePath(), fileName);
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(fileDestination);
			IOUtils.copy(inputStream, output);
			result = true;
			logger.info("Release file copied to : " + fileDestination.getAbsolutePath());
		} catch (final IOException e) {
			logger.warn("Error copying release file to " + sctDataFolder + ". Nested exception is : \n" + e.fillInStackTrace());
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(output);
		}
		// get the last yyymmdd fragment which indicates release data
		final String[] tokens = Files.getNameWithoutExtension(fileName).split("_");
		final String releaseDate = tokens[tokens.length-1];
		// if we are here then release date is a valid date format
		// now call loadSnomedData method passing release zip, if there is no matching database
		if (!releaseSchemaNameLookup.keySet().contains(releaseDate) ) {
			logger.info("Loading data into schema " + RVF_DB_PREFIX + releaseDate);
			final String schemaName = loadSnomedData(releaseDate, true, fileDestination);
			logger.info("schemaName = " + schemaName);
			// now add to releaseSchemaNameLookup
			releaseSchemaNameLookup.put(releaseDate, schemaName);
			result = true;
		}
		return result;
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
	public boolean uploadPublishedReleaseData(final File releasePackZip, final String version, final boolean isApppend) {
		boolean result = false;
		try(InputStream inputStream = new FileInputStream(releasePackZip)) {
			 result = uploadPublishedReleaseData(inputStream, releasePackZip.getName(), version, isApppend);
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
	 * @param purgeExisting boolean that controls if existing database needs to be purged
	 * @param zipDataFile the zip file that contains the release data
	 * @return the name of the schema to which the data has been loaded.
	 */
	@Override
	public String loadSnomedData(final String versionName, final boolean purgeExisting, final File... zipDataFile) {

		final long startTime = Calendar.getInstance().getTimeInMillis();
		final File outputFolder = new File(FileUtils.getTempDirectoryPath(), "rvf_loader_data_" + versionName);
		logger.info("Setting output folder location = " + outputFolder.getAbsolutePath());
		if (outputFolder.exists()) {
			logger.info("Output folder already exists and will be deleted before recreating.");
			outputFolder.delete();
		} 
		outputFolder.mkdir();
		final String createdSchemaName = RVF_DB_PREFIX + versionName;
		try {
			boolean alreadyExists = false;
			// first verify if database with name already exists, if it does then we skip
			final ResultSet catalogs = snomedDataSource.getConnection().getMetaData().getCatalogs();
			while (catalogs.next()) {
				final String schemaName = catalogs.getString(1);
				if ((createdSchemaName).equals(schemaName)) {
					alreadyExists = true;
					break;
				}
			}
			catalogs.close();

			if (alreadyExists && !purgeExisting) {
				return createdSchemaName;
			}
			
			// extract SNOMED CT content from zip file
			final Connection connection = snomedDataSource.getConnection();
			connection.setAutoCommit(true);
			createDBAndTables(versionName, connection);
			for (final File zipFile : zipDataFile) {
				ZipFileUtils.extractFilesFromZipToOneFolder(zipFile, outputFolder.getAbsolutePath());
			}
			loadReleaseFilesToDB(versionName, outputFolder, connection);

//			loadDataViaScript(versionName, outputFolder);

			
			// add schema name to look up map
			releaseSchemaNameLookup.put(versionName, createdSchemaName);
		} catch (final SQLException e) {
			logger.error("Error creating connection to database. Nested exception is : " + e.fillInStackTrace());
		} catch (final IOException e) {
			e.printStackTrace();
			logger.error("Unable to read sql file. Nested exception is : " + e.fillInStackTrace());
		} finally {
			// remove output directory so it does not occupy space
			FileUtils.deleteQuietly(outputFolder);
		}

		logger.info("Finished loading of data in : " + ((Calendar.getInstance().getTimeInMillis() - startTime) / 60000) + " minutes.");
		return createdSchemaName;
	}

	private void createDBAndTables(final String versionName, final Connection connection) throws SQLException, IOException {
		//clean and create database
		final String dropStr = "drop database if exists " + RVF_DB_PREFIX + versionName + ";";
		final String createDbStr = "create database if not exists " + RVF_DB_PREFIX + versionName + ";";
		final String  useStr = "use " + RVF_DB_PREFIX + versionName + ";";
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

	private void loadReleaseFilesToDB(final String versionName, final File rf2TextFilesDir, final Connection connection) throws SQLException, FileNotFoundException {
		
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

	private void loadDataViaScript(final String versionName,
			final File outputFolder) throws IOException, SQLException,
			FileNotFoundException {
		// get file from jar and write to tmp directory, so we can prepend sql statements and set default schema
		final InputStream is = getClass().getResourceAsStream("/sql/create-tables-mysql.sql");

		final File outputFile = new File(outputFolder.getAbsolutePath(), "create-tables-mysql.sql");
		// add scheme information
		FileUtils.writeStringToFile(outputFile, "drop database if exists " + RVF_DB_PREFIX + versionName + ";\n", true);
		FileUtils.writeStringToFile(outputFile, "create database if not exists " + RVF_DB_PREFIX + versionName + ";\n", true);
		FileUtils.writeStringToFile(outputFile, "use " + RVF_DB_PREFIX + versionName + ";\n", true);
		FileUtils.writeLines(outputFile, IOUtils.readLines(is), true);

		final InputStream is2 = getClass().getResourceAsStream("/sql/load-data-mysql.sql");
		final File outputFile2 = new File(outputFolder.getAbsolutePath(), "load-data-mysql.sql");
		FileUtils.writeStringToFile(outputFile2, "use " + RVF_DB_PREFIX + versionName + ";\n", true);
		for (String line : IOUtils.readLines(is2)) {
			// process line and add to output file
			line = line.replaceAll("<release_version>", versionName);
			line = line.replaceAll("<data_location>", outputFolder.getAbsolutePath());
			FileUtils.writeStringToFile(outputFile2, line + "\n", true);
		}

		logger.info("Executing script located at : " + outputFile.getAbsolutePath());
		final Connection connection = snomedDataSource.getConnection();
		final ScriptRunner runner = new ScriptRunner(connection);
		final InputStreamReader reader = new InputStreamReader(new FileInputStream(outputFile));
		runner.runScript(reader);
		reader.close();

		logger.info("Executing script located at : " + outputFile2.getAbsolutePath());
		final InputStreamReader reader2 = new InputStreamReader(new FileInputStream(outputFile2));
		runner.runScript(reader2);
		reader2.close();
		connection.close();
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
					logger.warn("More than one zip files having version:" + knownVersion);
				}
				return zipFiles[0];
			}
		}
		return null;
	}
}
