package org.ihtsdo.rvf.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.core.service.config.AssertionsResourceConfig;
import org.ihtsdo.rvf.core.service.config.DataSourceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class DataResourceConfig {

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${rvf.master.schema.name}")
    private String rvfMasterSchemaName;

    @Autowired
    private AssertionsResourceConfig assertionsResourceConfig;
    
    @Bean(name = "dataSource")
    public BasicDataSource getDataSource(DataSourceProperties dataSourceProperties) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        basicDataSource.setDriverClassName(driverClassName);
        basicDataSource.setDefaultCatalog(rvfMasterSchemaName);
        
        dataSourceProperties.configureDataSource(basicDataSource);
        
        return basicDataSource;
    }

    @Bean(name = "assertionResourceManager")
    public ResourceManager assertionResourceManager() {
        return new ResourceManager(assertionsResourceConfig, null);
    }
}