package org.ihtsdo.rvf.config;

import jakarta.persistence.EntityManagerFactory;
import org.ihtsdo.rvf.App;
import org.ihtsdo.rvf.core.messaging.ValidationMessageListener;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.core.service.AssertionServiceImpl;
import org.ihtsdo.rvf.core.service.MysqlValidationService;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.ihtsdo.rvf.core.service.RvfDynamicDataSource;
import org.ihtsdo.rvf.core.service.SqlAssertionValidationService;
import org.ihtsdo.rvf.core.service.ValidationRunner;
import org.ihtsdo.rvf.core.service.duck.DuckAssertionService;
import org.ihtsdo.rvf.core.service.duck.DuckDbValidationService;
import org.ihtsdo.rvf.importer.RvfAssertionsDatabasePrimerService;
import org.ihtsdo.rvf.rest.controller.AssertionAdministrationController;
import org.ihtsdo.rvf.rest.controller.AssertionController;
import org.ihtsdo.rvf.rest.controller.AssertionGroupAdministrationController;
import org.ihtsdo.rvf.rest.controller.AssertionPackController;
import org.ihtsdo.rvf.rest.controller.ReleaseController;
import org.ihtsdo.rvf.rest.controller.TestUploadFileController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.repository.Repository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The application starts in DuckDB mode with no database. The datasource URL points at a
 * reserved address nothing listens on, so the context can only start by never connecting.
 */
@SpringBootTest(classes = {App.class}, properties = {
		"rvf.execution.engine=duckdb",
		"spring.datasource.url=jdbc:mysql://192.0.2.1:1/x",
		"spring.datasource.username=nobody",
		"spring.datasource.password=nothing"})
@PropertySource("classpath:/application-test.properties")
@ActiveProfiles("test")
@WebAppConfiguration
class DuckDbEngineContextTest {

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	void theContextStartsWithNoDataSourceJpaOrRepositories() {
		assertTrue(context.isRunning());
		// Any DataSource, not just RVF's: auto-configuration would build one from the same properties.
		assertEquals(0, context.getBeanNamesForType(DataSource.class).length);
		assertEquals(0, context.getBeanNamesForType(EntityManagerFactory.class).length);
		assertEquals(0, context.getBeanNamesForType(Repository.class).length);
		assertEquals(0, context.getBeanNamesForType(AssertionServiceImpl.class).length);
	}

	@Test
	void theStartupWorkThatNeedsMysqlIsNotRegistered() {
		assertEquals(0, context.getBeanNamesForType(ReleaseDataManager.class).length);
		assertEquals(0, context.getBeanNamesForType(RvfAssertionsDatabasePrimerService.class).length);
		assertEquals(0, context.getBeanNamesForType(RvfDynamicDataSource.class).length);
		assertEquals(0, context.getBeanNamesForType(MysqlValidationService.class).length);
	}

	@Test
	void theValidationPathResolvesToTheDuckDbServices() {
		assertEquals(1, context.getBeanNamesForType(ValidationMessageListener.class).length,
				"without it nothing drains the validation queue");
		assertEquals(1, context.getBeanNamesForType(ValidationRunner.class).length);
		assertEquals(1, context.getBeanNamesForType(SqlAssertionValidationService.class).length);
		assertEquals(DuckDbValidationService.class, context.getBean(SqlAssertionValidationService.class).getClass());
		assertEquals(1, context.getBeanNamesForType(AssertionService.class).length);
		assertEquals(DuckAssertionService.class, context.getBean(AssertionService.class).getClass());
	}

	@Test
	void theSubmissionAndCatalogueControllersArePresentAndTheMysqlAdministrationOnesAreNot() {
		assertEquals(1, context.getBeanNamesForType(TestUploadFileController.class).length);
		assertEquals(1, context.getBeanNamesForType(AssertionController.class).length);
		assertEquals(1, context.getBeanNamesForType(AssertionPackController.class).length);
		assertEquals(0, context.getBeanNamesForType(AssertionAdministrationController.class).length);
		assertEquals(0, context.getBeanNamesForType(AssertionGroupAdministrationController.class).length);
		assertEquals(0, context.getBeanNamesForType(ReleaseController.class).length);
	}
}
