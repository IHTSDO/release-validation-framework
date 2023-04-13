package org.ihtsdo.rvf.execution.service.util;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.execution.service.AssertionExecutionService;
import org.ihtsdo.rvf.execution.service.config.MysqlExecutionConfig;
import org.ihtsdo.rvf.importer.AssertionGroupImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MySqlQueryTransformer {

    private final Logger logger = LoggerFactory.getLogger(MySqlQueryTransformer.class);

    private static final String FAILED_TO_FIND_RVF_DB_SCHEMA = "Failed to find rvf db schema for ";

    private String deltaTableSuffix = "d";
    private String snapshotTableSuffix = "s";
    private String fullTableSuffix = "f";

    public List<String> transformSql(String[] parts, MysqlExecutionConfig config) throws ConfigurationException {
        List<String> result = new ArrayList<>();
        String prospectiveSchema = config.getProspectiveVersion();
        final String[] nameParts = config.getProspectiveVersion().split("_");
        String moduleId = (nameParts.length >= 2 ? AssertionGroupImporter.ProductName.toModuleId(nameParts[1]) : "NOT_SUPPLIED");
        String version = (nameParts.length >= 3 ? nameParts[2] : "NOT_SUPPLIED");

        String previousReleaseSchema = config.getPreviousVersion();
        String dependencyReleaseSchema = config.getExtensionDependencyVersion();

        //We need both these schemas to exist
        if (prospectiveSchema == null) {
            throw new ConfigurationException (FAILED_TO_FIND_RVF_DB_SCHEMA + prospectiveSchema);
        }

        if (config.isReleaseValidation() && !config.isFirstTimeRelease() && previousReleaseSchema == null) {
            throw new ConfigurationException (FAILED_TO_FIND_RVF_DB_SCHEMA + previousReleaseSchema);
        }
        for( String part : parts) {
            if ((part.contains("<PREVIOUS>") && previousReleaseSchema == null)
                    || (part.contains("<DEPENDENCY>") && dependencyReleaseSchema == null)) {
                continue;
            }

            logger.debug("Original sql statement: {}", part);
            // remove all SQL comments - //TODO might throw errors for -- style comments
            final Pattern commentPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
            part = commentPattern.matcher(part).replaceAll("");
            // replace all substitutions for exec
            part = part.replaceAll("<RUNID>", String.valueOf(config.getExecutionId()));
            part = part.replaceAll("<MODULEID>", moduleId);
            part = part.replaceAll("<VERSION>", version);
            // watch out for any 's that users might have introduced
            part = part.replaceAll("<PROSPECTIVE>", prospectiveSchema);
            part = part.replaceAll("<TEMP>", prospectiveSchema);
            if (previousReleaseSchema != null) {
                part = part.replaceAll("<PREVIOUS>", previousReleaseSchema);
            }
            if (dependencyReleaseSchema != null) {
                part = part.replaceAll("<DEPENDENCY>", dependencyReleaseSchema);
            }
            part = part.replaceAll("<DELTA>", deltaTableSuffix);
            part = part.replaceAll("<SNAPSHOT>", snapshotTableSuffix);
            part = part.replaceAll("<FULL>", fullTableSuffix);
            part.trim();
            logger.debug("Transformed sql statement: {}", part);
            result.add(part);
        }
        return result;
    }
}
