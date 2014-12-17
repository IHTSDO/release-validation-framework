package org.ihtsdo.rvf.execution.service.impl;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An implementation of a {@link org.ihtsdo.rvf.execution.service.ReleaseDataManager}. The method afterPropertiesSet must always be called
 * immediately after instantiating this class outside of Spring context. This class uses a 'data folder' as the place to
 * store all known releases.
 */
@Service
public class ReleaseDataManagerImpl implements ReleaseDataManager, InitializingBean{

    private final Logger logger = LoggerFactory.getLogger(ReleaseDataManagerImpl.class);
    private static final String RVF_DB_PREFIX = "rvf_int_";
    protected String sctDataLocation;
    protected File sctDataFolder;
    @Resource(name = "snomedDataSource")
    BasicDataSource snomedDataSource;
    Map<String, String> releaseFileNameLookup = new HashMap<>();
    Map<String, String> releaseSchemaNameLookup = new HashMap<>();

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
        if(!sctDataFolder.exists()){
            if(sctDataFolder.mkdirs()){
                logger.info("Created data folder at : " + sctDataLocation);
            }
            else{
                logger.error("Unable to create data folder at path : " + sctDataLocation);
                throw new IllegalArgumentException("Bailing out because data location can not be set to : " + sctDataLocation);
            }
        }

        logger.info("Using data location as :" + sctDataFolder.getAbsolutePath());
        // now populate releaseLookup map with existing releases - but ask not to purge existing databases
        populateLookupMap(false);
    }

    /**
     * Utility method that generates a map of all known releases based on the contents of the data folder.
     */
    protected void populateLookupMap(boolean purgeExistingDatabase){
        releaseFileNameLookup.clear();

        // now get list of existing RVF_INT like databases
        Set<String> rvfDatabases = new HashSet<>();
        try {
            ResultSet catalogs = snomedDataSource.getConnection().getMetaData().getCatalogs();
            while(catalogs.next())
            {
                String schemaName = catalogs.getString(1);
                if(schemaName.startsWith(RVF_DB_PREFIX)){
                    // get the last yyymmdd fragment which indicates release data
                    String releaseDate = schemaName.substring(RVF_DB_PREFIX.length());
                    try
                    {
                        // we convert to data to verify we have a pattern matching yyyyMMdd
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                        sdf.setLenient(false);
                        sdf.parse(releaseDate);
                        // if we are here then release date is a valid date format
                        logger.info("Registering existing schema as known release: " + schemaName);
                        releaseSchemaNameLookup.put(releaseDate, schemaName);
                    }
                    catch (ParseException e) {
                        logger.warn("Error processing file. Not a valid release date : " + schemaName);
                    }

                    // also store for later processing
                    rvfDatabases.add(schemaName);
                }
            }
            catalogs.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            logger.error("Error getting list of existing schemas. Nested exception is : \n" + e.fillInStackTrace());
        }

        Collection<File> releases = FileUtils.listFiles(sctDataFolder, new String[]{"zip"}, false);
        logger.info("Number of releases in SNOMED CT data folder = " + releases.size());
        // we only want zip files and we do not want to traverse the directory recursively
        for(File file :  releases)
        {
            String fileName = file.getName();
            logger.info("Processing file in SNOMED CT data folder : " + fileName);
            //todo this won't work for extensions!
            int index = fileName.indexOf("SnomedCT_Release_INT_");
            if(index > -1)
            {
                // get the last yyymmdd fragment which indicates release data
                String releaseDate = fileName.substring("SnomedCT_Release_INT_".length() , fileName.length() - 4);
                try
                {
                    // we convert to data to verify we have a pattern matching yyyyMMdd
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    sdf.setLenient(false);
                    sdf.parse(releaseDate);
                    // if we are here then release date is a valid date format
                    releaseFileNameLookup.put(releaseDate, file.getAbsolutePath());
                    // now call loadSnomedData method passing release zip, if there is no matching database
                    if(! releaseSchemaNameLookup.keySet().contains(RVF_DB_PREFIX + releaseDate) || purgeExistingDatabase){
                        logger.info("Loading data into schema " + RVF_DB_PREFIX+releaseDate);
                        String schemaName = loadSnomedData(releaseDate, purgeExistingDatabase, file);
                        logger.info("schemaName = " + schemaName);
                        // now add to releaseSchemaNameLookup
                        releaseSchemaNameLookup.put(releaseDate, schemaName);
                    }
                }
                catch (ParseException e) {
                    logger.warn("Error processing file. Not a valid release date : " + fileName);
                }
            }
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
    public boolean uploadPublishedReleaseData(InputStream inputStream, String fileName,
                                              boolean overWriteExisting, boolean purgeExistingDatabase) {
        boolean result = false;
        // copy release pack zip to data location
        logger.info("Receiving release data - " + fileName);
        File fileDestination = new File(sctDataFolder.getAbsolutePath(), fileName);
        try {
            if(overWriteExisting || !fileDestination.exists()){
                IOUtils.copy(inputStream, new FileOutputStream(fileDestination));
                logger.info("Release file copied to : " + fileDestination.getAbsolutePath());
                result = true;
            } else {
                // verify release pack exists
                if(fileDestination.exists()){
                    logger.info("Release file already exists at : " + fileDestination.getAbsolutePath());
                    result = true;
                } else {
                	throw new RuntimeException ("Unexpected logic condition");
                }
            }

            // regenerate releaseLookup which takes care of data loading if the release not been loaded into the database
            populateLookupMap(purgeExistingDatabase);
        }
        catch (IOException e) {
            e.printStackTrace();
            logger.warn("Error copying release file to " + sctDataFolder +". Nested exception is : \n" + e.fillInStackTrace());
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
     */
    @Override
    public boolean uploadPublishedReleaseData(File releasePackZip, boolean overWriteExisting, boolean purgeExistingDatabase){
        boolean result = false;
        // copy release pack zip to data location
        try {
            File fileDestination = new File(sctDataFolder.getAbsolutePath(), releasePackZip.getName());
            if(overWriteExisting || !fileDestination.exists()){
                FileUtils.copyFile(releasePackZip, fileDestination);
                logger.info("Release file copied to : " + fileDestination);
                result = true;
            } else {
                // verify release pack exists
                if(fileDestination.exists()){
                    logger.info("Release file already exists at : " + fileDestination.getAbsolutePath());
                    result = true;
                } else {
                	throw new RuntimeException ("Unexpected logic condition");
                }
            }
            // regenerate releaseLookup which takes care of data loading if the release not been loaded into the database
            populateLookupMap(purgeExistingDatabase);
        }
        catch (IOException e) {
            e.printStackTrace();
            logger.warn("Error copying release file to " + sctDataFolder +". Nested exception is : \n" + e.fillInStackTrace());
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
    public String loadSnomedData(String versionName, boolean purgeExisting, File zipDataFile){

        Calendar startTime = Calendar.getInstance();
        File outputFolder = new File(FileUtils.getTempDirectoryPath(), "rvf_loader_data_"+versionName);
        logger.info("Setting output folder location = " + outputFolder.getAbsolutePath());
        if(! outputFolder.exists() && outputFolder.isDirectory()) {
            outputFolder.mkdir();
        } else {
            logger.info("Output folder already exists");
        }

        String createdSchemaName = RVF_DB_PREFIX+versionName;
        try
        {
            boolean alreadyExists = false;
            // first verify if database with name already exists, if it does then we skip
            ResultSet catalogs = snomedDataSource.getConnection().getMetaData().getCatalogs();
            while(catalogs.next())
            {
                String schemaName = catalogs.getString(1);
                if((createdSchemaName).equals(schemaName)){
                    alreadyExists = true;
                    break;
                }
            }
            catalogs.close();

            if(alreadyExists && !purgeExisting){
                return createdSchemaName;
            }

            // get file from jar and write to tmp directory, so we can prepend sql statements and set default schema
            InputStream is = getClass().getResourceAsStream("/sql/create-tables-mysql.sql");

            File outputFile = new File(outputFolder.getAbsolutePath(), "create-tables-mysql.sql");
            // add scheme information
            FileUtils.writeStringToFile(outputFile, "drop database if exists "+RVF_DB_PREFIX+versionName+";\n", true);
            FileUtils.writeStringToFile(outputFile, "create database if not exists "+RVF_DB_PREFIX+versionName+";\n", true);
            FileUtils.writeStringToFile(outputFile, "use "+RVF_DB_PREFIX+versionName+";\n", true);
            FileUtils.writeLines(outputFile, IOUtils.readLines(is), true);

            InputStream is2 = getClass().getResourceAsStream("/sql/load-data-mysql.sql");
            File outputFile2 = new File(outputFolder.getAbsolutePath(), "load-data-mysql.sql");
            FileUtils.writeStringToFile(outputFile2, "use "+RVF_DB_PREFIX+versionName+";\n", true);
            for(String line : IOUtils.readLines(is2))
            {
                // process line and add to output file
                line = line.replaceAll("<release_version>", versionName);
                line = line.replaceAll("<data_location>", outputFolder.getAbsolutePath());
                FileUtils.writeStringToFile(outputFile2, line+"\n", true);
            }

            // extract SNOMED CT content from zip file
            extractZipFile(zipDataFile, outputFolder.getAbsolutePath());

            logger.info("Executing script located at : " + outputFile.getAbsolutePath());
            Connection connection = snomedDataSource.getConnection();
            ScriptRunner runner = new ScriptRunner(connection);
            InputStreamReader reader = new InputStreamReader(new FileInputStream(outputFile));
            runner.runScript(reader);
            reader.close();

            logger.info("Executing script located at : " + outputFile2.getAbsolutePath());
            InputStreamReader reader2 = new InputStreamReader(new FileInputStream(outputFile2));
            runner.runScript(reader2);
            reader2.close();
            connection.close();

            // add schema name to look up map
            releaseSchemaNameLookup.put(versionName, createdSchemaName);
        }
        catch (SQLException e) {
            logger.error("Error creating connection to database. Nested exception is : " + e.fillInStackTrace());
        }
        catch (IOException e) {
            logger.error("Unable to read sql file. Nested exception is : " + e.fillInStackTrace());
        }
        finally {
            // remove output directory so it does not occupy space
            FileUtils.deleteQuietly(outputFolder);
        }

        logger.info("Finished loading of data in : " + ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())/60000) + " minutes.");
        return createdSchemaName;
    }

    /**
     * Utility method for extracting a zip file to a given folder
     * @param file the zip file to be extracted
     * @param outputDir the output folder to extract the zip to.
     */
    protected void extractZipFile(File file, String outputDir){
        try {
            ZipFile zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(outputDir,  entry.getName());
                entryDestination.getParentFile().mkdirs();
                if (entry.isDirectory())
                    entryDestination.mkdirs();
                else {
                    InputStream in = zipFile.getInputStream(entry);
                    OutputStream out = new FileOutputStream(entryDestination);
                    IOUtils.copy(in, out);
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
            }

            // now rename any files with prefix x - possibly pre-release files and flatten directory for easy sql processing
            renameFilesinFolder(new File(outputDir), new File(outputDir), "x");
        }
        catch (IOException e) {
            e.printStackTrace();
            logger.warn("Nested exception is : " + e.fillInStackTrace());
        }
    }

    /**
     * Utility method for removing the given prefix in all pre release file names and flattening the directory structure
     *
     * @param sourceFolder the source folder containing the files to process (recursive)
     * @param targetFolder the target folder that should contain the final files
     */
    private void renameFilesinFolder(File sourceFolder, File targetFolder, String prefix){
        for(File f: sourceFolder.listFiles())
        {
            if(f.isDirectory()){
                renameFilesinFolder(f, targetFolder, prefix);
            }
            else{
                String fileName = f.getName();
                if(fileName.startsWith(prefix)){
                    fileName = fileName.substring(1);
                }

                // move file to flattened structure
                try {
                    FileUtils.moveFile(f, new File(targetFolder, fileName));
                    logger.info("fileName = " + fileName);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Verifies if the given releaseVersion has already been loaded into a database.
     *
     * @param releaseVersion the release date as a yyyymmdd string (e.g. 20140731)
     * @return if a schema for the version already exists
     */
    @Override
    public boolean isKnownRelease(String releaseVersion){
        return releaseSchemaNameLookup.containsKey(releaseVersion);
    }

    /**
     * Returns a list of all known releases that have been uploaded into the database
     * @return set of all releases
     */
    @Override
    public Set<String> getAllKnownReleases(){
        return releaseSchemaNameLookup.keySet();
    }

    public void setSctDataLocation(String sctDataLocation) {
        this.sctDataLocation = sctDataLocation;
    }

    public void setSnomedDataSource(BasicDataSource snomedDataSource) {
        this.snomedDataSource = snomedDataSource;
    }

    /**
     * Returns the schema name that corresponds to  the given release.
     * @param releaseVersion the release date as a yyyymmdd string (e.g. 20140731)
     * @return the corresponding schema name
     */
    @Override
    public String getSchemaForRelease(String releaseVersion){
        return releaseSchemaNameLookup.get(releaseVersion);
    }

    /**
     * Sets the schema name that corresponds to  the given release.
     * @param releaseVersion the release date as a yyyymmdd string (e.g. 20140731)
     * @param schemaName the corresponding schema name
     */
    @Override
    public void setSchemaForRelease(String releaseVersion, String schemaName){
        releaseSchemaNameLookup.put(releaseVersion, schemaName);
    }
}
