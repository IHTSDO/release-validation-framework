package org.ihtsdo.rvf.execution.service.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * A wrapper around {@link org.apache.commons.dbcp.BasicDataSource} that handles dynamic schema changes
 */
@Service
public class RvfDynamicDataSource {

	@Value("${rvf.jdbc.driverClassName}") 
	private String driverClassName;

    @Value("${rvf.jdbc.url}") 
    private String url;

    @Value("${rvf.jdbc.username}") 
    private String username;

    @Value("${rvf.jdbc.password}") 
    private String password;
    
	private ConcurrentHashMap<String, BasicDataSource> schemaDatasourceMap = new ConcurrentHashMap<>();

	private final Logger LOGGER = LoggerFactory.getLogger(RvfDynamicDataSource.class);

	/**
	 * Returns a connection for the given schema. It uses an underlying map to store relevant {@link org.apache.commons.dbcp.BasicDataSource}
	 * @param schema the schema for which the connection needs to be returned
	 * @return the connection for this schema
	 * @throws SQLException
	 */
	public Connection getConnection(String schema) throws SQLException {
		if(schemaDatasourceMap.containsKey(schema)){
			LOGGER.debug("get connection for schema:" + schema);
			return schemaDatasourceMap.get(schema).getConnection();
		}
		else{
			BasicDataSource dataSource = createDataSource(schema);
			// add to map
			schemaDatasourceMap.putIfAbsent(schema, dataSource);
			LOGGER.debug("Create datasource for schema:" + schema);
			return dataSource.getConnection();
		}
	}
	
	public BasicDataSource createDataSource(String schema) {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setDriverClassName(driverClassName);
		dataSource.setDefaultCatalog(schema);
		dataSource.setTestOnBorrow(true);
		dataSource.setTestOnReturn(true);
		dataSource.setTestWhileIdle(true);
		dataSource.setValidationQuery("SELECT 1");
		dataSource.setMinEvictableIdleTimeMillis(1800000);
		dataSource.setTimeBetweenEvictionRunsMillis(1800000);
		//READ_COMMITTED
		dataSource.setDefaultTransactionIsolation(2);
		return dataSource;
	}


	public void close( String schema) {
		if ( schema != null) {
			BasicDataSource dataSource = schemaDatasourceMap.get(schema);
			if (dataSource != null) {
				try {
					dataSource.close();
				} catch (SQLException e) {
					LOGGER.error("Failed to close datasource for schema:" + schema, e);
				}
			}
			schemaDatasourceMap.remove(schema);
			LOGGER.debug("Close and remove datasource from map for schema:" + schema);
		}
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
