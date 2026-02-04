package org.ihtsdo.rvf.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.rvf.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import javax.sql.DataSource;

import java.sql.Connection;

@Testcontainers
@SpringBootTest(classes = {App.class})
@PropertySource("classpath:/application-test.properties")
@ActiveProfiles("test")
@WebAppConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class IntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);
	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Container
	public static MySQLContainer mySQLContainer = new TestMySQLContainer();

	@Autowired
	private DataSource dataSource;

	@DynamicPropertySource
	static void mysqlProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
		registry.add("spring.datasource.username", mySQLContainer::getUsername);
		registry.add("spring.datasource.password", mySQLContainer::getPassword);
	}

	protected String getSpringBootUrl() {
		try (Connection conn = dataSource.getConnection()) {
			return conn.getMetaData().getURL();
		} catch (Exception e) {
			LOGGER.error("Error getting url", e);
			return null;
		}
	}

	protected String getTestcontainersUrl() {
		return mySQLContainer.getJdbcUrl();
	}

	protected String getResponseBody(ResultActions resultActions) {
		try {
			return resultActions.andReturn().getResponse().getContentAsString();
		} catch (Exception e) {
			return null;
		}
	}

	protected <T> T getResponseBody(ResultActions resultActions, Class<T> clazz) {
		try {
			String responseBody = resultActions.andReturn().getResponse().getContentAsString();
			return OBJECT_MAPPER.readValue(responseBody, clazz);
		} catch (Exception e) {
			return null;
		}
	}

	protected <T> T getResponseBody(ResultActions resultActions, TypeReference<T> typeReference) {
		try {
			String responseBody = resultActions.andReturn()
					.getResponse()
					.getContentAsString();
			return OBJECT_MAPPER.readValue(responseBody, typeReference);
		} catch (Exception e) {
			return null;
		}
	}
}
