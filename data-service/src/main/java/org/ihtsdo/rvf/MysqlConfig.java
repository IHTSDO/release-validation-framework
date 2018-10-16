package org.ihtsdo.rvf;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@PropertySources({
	@PropertySource(value = "classpath:data-service-defaults.properties"),
	@PropertySource(value = "file:${rvfConfigLocation}/data-service.properties", ignoreResourceNotFound=true)})
@EntityScan("org.ihtsdo.rvf.entity")
@SpringBootApplication
@EnableJpaRepositories
public class MysqlConfig {
	
	@Value("${rvf.jdbc.driverClassName}") 
	private String driverClassName;

    @Value("${rvf.jdbc.url}") 
    private String url;

    @Value("${rvf.jdbc.username}") 
    private String username;

    @Value("${rvf.jdbc.password}") 
    private String password;
    
    @Value("${rvf.master.schema.name}")
    private String defaultSchemaName;

    @Bean(name = "dataSource")
    public BasicDataSource getDataSource() {
    	BasicDataSource basicDataSource = new BasicDataSource();
    	basicDataSource.setUrl(url);
    	basicDataSource.setUsername(username);
    	basicDataSource.setPassword(password);
    	basicDataSource.setDriverClassName(driverClassName);
    	basicDataSource.setDefaultCatalog(defaultSchemaName);
        return basicDataSource;
    }
}