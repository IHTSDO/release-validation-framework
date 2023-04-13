package au.csiro.datachecks.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Class that describes a set of test files to load for the datachecks. Each
 * instance of this class represents a related set of files usually
 * corresponding to one release format for one release of a terminology. For
 * each object, the following is represented
 * <ul>
 * <li>{@link #type} - the type, or release format of the files. i.e. rf1, rf2,
 * etc
 * <li>{@link #name} - a unique identifier for this set of files e.g.
 * "last international rf2 snapshot release"
 * <li>{@link #extracts} - a list of {@link Artifact}s where the sources of
 * these files can be found and where to put them to be tested
 * <li>{@link #substitutionMap} - a map containing name-value pairs of parameters required
 * by the schema file for this type (rf2, rf1 etc)
 * </ul>
 */
public class TestFileSet {
    private String type;
    private String name = "";
    private Map<String, String> substitutionMap = new TreeMap<>();
    private List<Artifact> extracts = new ArrayList<>();
    private ImportSpec importSpec;

    /**
     * @return the type of files represented by this {@link TestFileSet} - e.g.
     *         rf1, rf2...etc
     */
    public String getType() {
        return this.type;
    }

    /**
     * Sets the type of files represented by this {@link TestFileSet} - e.g.
     * rf1, rf2...etc
     * 
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the unique identifier for this {@link TestFileSet}
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the unique identifier for this {@link TestFileSet}
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the map of name-value pair parameters needed to replace tokens in
     *         the schema file for this type of file.
     */
    public Map<String, String> getSubstitutionMap() {
        return this.substitutionMap;
    }

    /**
     * Sets the map of name-value pair parameters needed to replace tokens in
     * the schema file for this type of file.
     * 
     * @param map
     */
    public void setSubstitutionMap(Map<String, String> map) {
        this.substitutionMap = map;
    }

    /**
     * @return the {@link Artifact} objects describing where the source of this
     *         {@link TestFileSet} are
     */
    public List<Artifact> getExtracts() {
        return this.extracts;
    }

    /**
     * Sets the {@link Artifact} objects describing where the source of this
     * {@link TestFileSet} are
     * 
     * @param locations
     */
    public void setExtracts(List<Artifact> locations) {
        this.extracts = locations;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public ImportSpec getImportSpec() {
        return importSpec;
    }

    public void setImportSpec(ImportSpec importSpec) {
        this.importSpec = importSpec;
    }
}
