package org.ihtsdo.rvf.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.rvf.core.service.config.AssertionsResourceConfig;
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

    @Value("${rvf.datasource.maxActive:200}")
    private int maxActive;

    @Value("${rvf.datasource.maxWait:20000}")
    private long maxWait;

    @Value("${rvf.datasource.testOnBorrow:true}")
    private boolean testOnBorrow;

    @Value("${rvf.datasource.testWhileIdle:true}")
    private boolean testWhileIdle;

    @Value("${rvf.datasource.validationQuery:SELECT 1}")
    private String validationQuery;

    @Value("${rvf.datasource.defaultTransactionIsolation:2}")
    private int defaultTransactionIsolation;

    @Value("${rvf.datasource.timeBetweenEvictionRunsMillis:60000}")
    private long timeBetweenEvictionRunsMillis;

    @Value("${rvf.datasource.minEvictableIdleTimeMillis:1800000}")
    private long minEvictableIdleTimeMillis;

    @Value("${rvf.datasource.numTestsPerEvictionRun:3}")
    private int numTestsPerEvictionRun;

    @Autowired
    private AssertionsResourceConfig assertionsResourceConfig;
    
    @Bean(name = "dataSource")
    public BasicDataSource getDataSource() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        basicDataSource.setDriverClassName(driverClassName);
        basicDataSource.setDefaultCatalog(rvfMasterSchemaName);
        basicDataSource.setTestOnBorrow(testOnBorrow);
        basicDataSource.setTestWhileIdle(testWhileIdle);
        basicDataSource.setMaxActive(maxActive);
        basicDataSource.setMaxWait(maxWait);
        basicDataSource.setValidationQuery(validationQuery);
        basicDataSource.setDefaultTransactionIsolation(defaultTransactionIsolation);
        
        // Idle connection eviction settings to prevent stale connections
        // Evict connections idle for configured time (default: 30 minutes, well before MySQL's 8-hour wait_timeout)
        basicDataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        basicDataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        basicDataSource.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        
        return basicDataSource;
    }

    @Bean(name = "assertionResourceManager")
    public ResourceManager assertionResourceManager() {
        return new ResourceManager(assertionsResourceConfig, null);
    }
}