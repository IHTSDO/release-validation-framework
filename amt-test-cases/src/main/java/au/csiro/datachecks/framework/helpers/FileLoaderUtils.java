package au.csiro.datachecks.framework.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import au.csiro.datachecks.framework.FileLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SQLExec;
import org.codehaus.plexus.util.FileUtils;

import au.csiro.datachecks.framework.DatabaseConnectionSettings;
import au.csiro.datachecks.framework.TestFileSet;

public class FileLoaderUtils {

    private static Logger log = Logger.getLogger(FileLoader.class.getName());

    public static void createTempSchemaDirectory(File tempSchemaDirectory) {
        try {
            FileUtils.forceMkdir(tempSchemaDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed setting up temp directory for schema files " + tempSchemaDirectory, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<File> getFilesForSchemaDirectory(File schemaDirectory) {
        try {
            return FileUtils.getFiles(schemaDirectory, "*.sql", "", true);
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to list files to determine available file types from " + schemaDirectory, e);
        } catch (IllegalStateException e) {
            // Log if schemaDirectory does not exist as warning
            log.warning(e.getMessage());
            return null;
        }
    }

    /**
     * Determines if a file is a specific core component file
     * 
     * @param file The candidate file
     * @param fileSet If the candidate file is a match, it will be recorded as
     *            such in the TestFileSet.
     * @param containsPattern Tests for this text in the name of the file
     * @param tokenKey The substitution property to be set in the TestFileSet
     */
    public static boolean checkForCoreFile(File file, TestFileSet fileSet, String containsPattern, String tokenKey) {
        String filename = file.getName();
        if (!filename.matches(FileLoader.RF2_PREFIX_REGEX) && filename.matches(containsPattern)
                && filename.endsWith(".txt")) {
            Map<String, String> tokenMap = fileSet.getSubstitutionMap();
            if (tokenMap.containsKey(tokenKey)) {
                throw new RuntimeException(String.format("Failing processing file set %1$s. "
                        + "Encountered multiple matching core files for %2$s. Found '%3$s' and '%4$s'.",
                    fileSet.getName(), tokenKey, tokenMap.get(tokenKey), file.getAbsolutePath()));
            }
            tokenMap.put(tokenKey, file.getAbsolutePath());
            return true;
        }
        return false;
    }

    /**
     * Determines if a file is a refset type (of arbitrary type)
     * 
     * @param file The candidate file
     * @param fileSet If the candidate file is a match, it will be recorded as
     *            such in the TestFileSet.
     */
    public static void checkForRefsetFile(File file, TestFileSet fileSet) {
        String filename = file.getName();
        Map<String, String> tokenMap = fileSet.getSubstitutionMap();
        String filepath = file.getAbsolutePath();
        String key = null;

        if (filename.endsWith(".txt")) {
            if (filename.matches(FileLoader.RF2_PREFIX_REGEX) && filename.contains("Refset_") &&
                    !(filename.contains("Owl") || filename.contains("OWL"))) {
                int prefixIndex = filename.indexOf(FileLoader.RF2_PREFIX) + FileLoader.RF2_PREFIX.length();
                if (filename.contains("Stated")) {
                    key = "stated_"
                            + filename.substring(prefixIndex, filename.indexOf('_', prefixIndex)).concat(".file.path");
                } else {
                    key = filename.substring(prefixIndex, filename.indexOf('_', prefixIndex)).concat(".file.path");
                }
            } else if (filepath.matches(".*refsets(/|\\\\)clinical.*")) {
                key = "cRefset.file.path";
            } else if (filepath.matches(".*refsets(/|\\\\)structural.*")) {
                if (filename.contains("AttributeValue")) {
                    key = "cRefset.file.path";
                } else if (filename.contains("SimpleMap")) {
                    key = "sRefset.file.path";
                }
            }
        }
        if (key != null) {
            tokenMap.put(key, tokenMap.containsKey(key) ? tokenMap.get(key).concat(",").concat(filepath) : filepath);
        }
    }

    /**
     * Combine multiple files, of the same type, into one (for the sql load
     * statement)
     * 
     * @param srcFiles CSV list of fully qualified paths for files of the same
     *            type (eg cRefset files)
     * @param destFile The target destination file to be appended to
     * @return True if an data was written to the destination file
     */
    public static boolean aggregateFiles(String srcFiles, File destFile) throws IOException {
        boolean wasDataWritten = false;
        String destFilepath = destFile.getAbsolutePath();
        StringBuilder missingHeaders = new StringBuilder();
        for (String srcPath : srcFiles.split(",")) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcPath))));
            String line = reader.readLine(); // header
            if (!line.matches("^(id|identifierSchemeId)\\t.*")) {
                missingHeaders.append(srcPath);
                missingHeaders.append(",");
            }
            line = null;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0) {
                    FileUtils.fileAppend(destFilepath, line + "\r\n");
                    wasDataWritten = true;
                }
            }
            reader.close();
        }

        if (!missingHeaders.toString().equals("")) {
            throw new RuntimeException("Following files are missing header rows " + missingHeaders.toString());
        }
        return wasDataWritten;
    }

    public static void executeLoadScript(File sqlFile, String delimiter,
            DatabaseConnectionSettings databaseConnectionSettings) {
        try {
          SQLExec executer = new SQLExec();
          executer.setProject(new Project());
          if ((delimiter != null) && (!delimiter.isEmpty())) {
              executer.setDelimiter(delimiter);
          }
          executer.setSrc(sqlFile);
          executer.setDriver(databaseConnectionSettings.getDriver());
          executer.setPassword(databaseConnectionSettings.getPassword());
          executer.setUserid(databaseConnectionSettings.getUser());
          executer.setUrl(databaseConnectionSettings.getUrl());
          log.info("Running script " + sqlFile.getAbsolutePath());
          executer.execute();  
        } catch (Exception e) {
          throw new RuntimeException("Error loading SQL file (" + sqlFile + ") with delimiter (" + delimiter + ")", e);
        }
    }

}
