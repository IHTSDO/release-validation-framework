package au.csiro.datachecks.framework;

/**
 * Database connection details data object class - used to configure the database connection
 * and pass in from Maven or Ant
 */
public class DatabaseConnectionSettings {
    private String driver;
    private String password;
    private String user;
    private String url;
    private String schema;

    /**
     * @param driver name of the jdbc driver class
     * @param password for authentication
     * @param user username for authentication
     * @param url jdbc connection url string for the database
     * @param schema schema name in the database
     */
    public DatabaseConnectionSettings(String driver, String password, String user, String url, String schema) {
        this.driver = driver;
        this.password = password;
        this.user = user;
        this.url = url;
        this.schema = schema;
    }

    /**
     * @return jdbc driver class name
     */
    public String getDriver() {
        return this.driver;
    }

    /**
     * @return password used for authentication
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @return username used for authentication
     */
    public String getUser() {
        return this.user;
    }

    /**
     * @return jdbc conncetion url for the database
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * @return schema in the database to use
     */
    public String getSchema() {
        return this.schema;
    }
}
