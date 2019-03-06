package org.ihtsdo.rvf.execution.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A wrapper around {@link org.apache.commons.dbcp.BasicDataSource} that handles dynamic schema changes
 */
@Service
public class RvfDynamicDataSource {

	@Resource(name = "dataSource")
	private BasicDataSource dataSource;
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
		BasicDataSource dataSource = createDataSource(schema);
		schemaDatasourceMap.putIfAbsent(schema, dataSource);
		LOGGER.debug("Datasource created for schema:" + schema);
		return dataSource.getConnection();
	}
	
	public BasicDataSource createDataSource(String schema) {
		BasicDataSource newDataSource = new BasicDataSource();
		newDataSource.setUrl(dataSource.getUrl());
		newDataSource.setUsername(dataSource.getUsername());
		newDataSource.setPassword(dataSource.getPassword());
		newDataSource.setDriverClassName(dataSource.getDriverClassName());
		newDataSource.setDefaultCatalog(schema);
		newDataSource.setTestOnBorrow(true);
		newDataSource.setTestOnReturn(true);
		newDataSource.setTestWhileIdle(true);
		newDataSource.setValidationQuery("SELECT 1");
		newDataSource.setMaxActive(dataSource.getMaxActive());
		newDataSource.setMaxWait(dataSource.getMaxWait());
		newDataSource.setDefaultTransactionIsolation(dataSource.getDefaultTransactionIsolation());
		return newDataSource;
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
}
