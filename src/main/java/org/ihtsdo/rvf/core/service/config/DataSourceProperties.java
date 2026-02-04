package org.ihtsdo.rvf.core.service.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for RVF datasource settings.
 * This eliminates duplication of @Value annotations across multiple classes.
 */
@Configuration
@ConfigurationProperties(prefix = "rvf.datasource")
public class DataSourceProperties {

	private int maxActive = 200;
	private long maxWait = 20000;
	private boolean testOnBorrow = true;
	private boolean testWhileIdle = true;
	private String validationQuery = "SELECT 1";
	private int defaultTransactionIsolation = 2;
	private long timeBetweenEvictionRunsMillis = 60000;
	private long minEvictableIdleTimeMillis = 1800000;
	private int numTestsPerEvictionRun = 3;

	public int getMaxActive() {
		return maxActive;
	}

	public void setMaxActive(int maxActive) {
		this.maxActive = maxActive;
	}

	public long getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	public boolean isTestOnBorrow() {
		return testOnBorrow;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}

	public boolean isTestWhileIdle() {
		return testWhileIdle;
	}

	public void setTestWhileIdle(boolean testWhileIdle) {
		this.testWhileIdle = testWhileIdle;
	}

	public String getValidationQuery() {
		return validationQuery;
	}

	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	public int getDefaultTransactionIsolation() {
		return defaultTransactionIsolation;
	}

	public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}

	public long getTimeBetweenEvictionRunsMillis() {
		return timeBetweenEvictionRunsMillis;
	}

	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	public long getMinEvictableIdleTimeMillis() {
		return minEvictableIdleTimeMillis;
	}

	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	public int getNumTestsPerEvictionRun() {
		return numTestsPerEvictionRun;
	}

	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		this.numTestsPerEvictionRun = numTestsPerEvictionRun;
	}

	/**
	 * Configures common datasource properties on a BasicDataSource instance.
	 * This method applies all the configuration properties from this class to the given datasource.
	 * 
	 * @param dataSource the BasicDataSource to configure
	 */
	public void configureDataSource(BasicDataSource dataSource) {
		dataSource.setTestOnBorrow(testOnBorrow);
		dataSource.setTestWhileIdle(testWhileIdle);
		dataSource.setMaxActive(maxActive);
		dataSource.setMaxWait(maxWait);
		dataSource.setValidationQuery(validationQuery);
		dataSource.setDefaultTransactionIsolation(defaultTransactionIsolation);
		
		// Idle connection eviction settings to prevent stale connections
		// Evict connections idle for configured time (default: 30 minutes, well before MySQL's 8-hour wait_timeout)
		dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		dataSource.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
	}
}
