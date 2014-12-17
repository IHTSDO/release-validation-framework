package org.ihtsdo.rvf.execution.service.util;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around {@link org.apache.commons.dbcp.BasicDataSource} that handles dynamic schema changes
 */
@Service
public class RvfDynamicDataSource {

    private String url;
    @Resource(name = "snomedDataSource")
    BasicDataSource basicDataSource;
    private Map<String, DataSource> schemaDatasourceMap = new HashMap<>();

    /**
     * Returns a connection for the given schema. It uses an underlying map to store relevant {@link org.apache.commons.dbcp.BasicDataSource}
     * so datasources are reused
     * @param schema the schema for which the connection needs to be returned
     * @return the connection for this schema
     * @throws SQLException
     */
    public Connection getConnection(String schema) throws SQLException {
        if(schemaDatasourceMap.containsKey(schema)){
            return schemaDatasourceMap.get(schema).getConnection();
        }
        else{
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setUrl(url);
            dataSource.setUsername(basicDataSource.getUsername());
            dataSource.setPassword(basicDataSource.getPassword());
            dataSource.setDriverClassName(basicDataSource.getDriverClassName());
            dataSource.setDefaultCatalog(schema);
            // add to map
            schemaDatasourceMap.put(schema, dataSource);

            return dataSource.getConnection();
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
