package org.ihtsdo.rvf.execution.service.util;

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

	private String url;
	@Resource(name = "snomedDataSource")
	private BasicDataSource basicDataSource;
	private ConcurrentHashMap<String, BasicDataSource> schemaDatasourceMap = new ConcurrentHashMap<>();

	private final Logger LOGGER = LoggerFactory.getLogger(RvfDynamicDataSource.class);

	/**
	 * Returns a connection for the given schema. It uses an underlying map to store relevant {@link org.apache.commons.dbcp.BasicDataSource}
	 * so datasources are reused
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
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl(url);
			dataSource.setUsername(basicDataSource.getUsername());
			dataSource.setPassword(basicDataSource.getPassword());
			dataSource.setDriverClassName(basicDataSource.getDriverClassName());
			dataSource.setDefaultCatalog(schema);
			dataSource.setMaxActive(basicDataSource.getMaxActive());
			dataSource.setMaxIdle(basicDataSource.getMaxIdle());
			dataSource.setMinIdle(basicDataSource.getMinIdle());
			dataSource.setTestOnBorrow(basicDataSource.getTestOnBorrow());
			dataSource.setTestOnReturn(basicDataSource.getTestOnReturn());
			dataSource.setTestWhileIdle(basicDataSource.getTestWhileIdle());
			dataSource.setValidationQuery(basicDataSource.getValidationQuery());
			dataSource.setValidationQueryTimeout(basicDataSource.getValidationQueryTimeout());
			dataSource.setMinEvictableIdleTimeMillis(basicDataSource.getMinEvictableIdleTimeMillis());
			dataSource.setTimeBetweenEvictionRunsMillis(basicDataSource.getTimeBetweenEvictionRunsMillis());
			// add to map
			schemaDatasourceMap.putIfAbsent(schema, dataSource);
			LOGGER.debug("Create datasource for schema:" + schema);
			return dataSource.getConnection();
		}
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
