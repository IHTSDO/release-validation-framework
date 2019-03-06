package org.ihtsdo.rvf;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@PropertySources({
	@PropertySource(value = "classpath:data-service-defaults.properties"),
	@PropertySource(value = "file:${rvfConfigLocation}/data-service.properties", ignoreResourceNotFound=true)})
@EntityScan("org.ihtsdo.rvf.entity")
@EnableJpaRepositories
@EnableTransactionManagement
public class DataServiceConfig {
	
	@Value("${rvf.jdbc.driverClassName}") 
	private String driverClassName;
	
	@Value("${rvf.jdbc.url}") 
	private String url;

	@Value("${rvf.jdbc.username}") 
	private String username;
	
	@Value("${rvf.jdbc.password}") 
	private String password;
	
	@Value("${rvf.master.schema.name}")
	private String rvfMasterSchemaName;

	@Bean(name = "dataSource")
	public BasicDataSource getDataSource() {
		BasicDataSource basicDataSource = new BasicDataSource();
		basicDataSource.setUrl(url);
		basicDataSource.setUsername(username);
		basicDataSource.setPassword(password);
		basicDataSource.setDriverClassName(driverClassName);
		basicDataSource.setDefaultCatalog(rvfMasterSchemaName);
		basicDataSource.setTestOnBorrow(true);
		basicDataSource.setTestWhileIdle(true);
		basicDataSource.setMaxActive(200);
		basicDataSource.setMaxWait(20000);
		basicDataSource.setValidationQuery("SELECT 1");
		basicDataSource.setDefaultTransactionIsolation(2);
		return basicDataSource;
	}
}