package org.ihtsdo.rvf.configuration;

import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

/**
 * Configuration for running a MySQL container for integration tests.
 */
public class TestMySQLContainer extends MySQLContainer {
	private static final String FULL_IMAGE_NAME = "mysql:8.0";
	private static final String DB_NAME = "rvf_master";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "root";
	private static final String CMD_ALLOW_LOCAL_DATA = "--local-infile=1";

	public TestMySQLContainer() {
		super(DockerImageName.parse(FULL_IMAGE_NAME));
		super.withDatabaseName(DB_NAME);
		super.withUsername(DB_USER);
		super.withPassword(DB_PASSWORD);
		super.withCommand(CMD_ALLOW_LOCAL_DATA);
		super.withReuse(true);
		super.withEnv("MYSQL_ROOT_PASSWORD", "root");
		super.withInitScript("init.sql");
		super.urlParameters = Map.of(
				"allowLoadLocalInfile", "true",
				"useSSL", "false",
				"allowPublicKeyRetrieval", "true",
				"sessionVariables", "sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION'"

		);

		/*
		 * Each customization ( e.g. super.with(...) ) forces Testcontainers to re-create the MySQL container for each test class.
		 * As a result, the JDBC URL changes and Spring Boot loses the connection to the database. To get around this whilst keeping
		 * RVF required customizations, hardcode a port binding. Spring Boot keeps the connection to the database, albeit,
		 * technically the database is running on a new container each time.
		 * */
		super.setPortBindings(List.of("50848:3306"));
	}
}
