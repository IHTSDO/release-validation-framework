package org.ihtsdo.rvf.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * The Testcontainers MySQL container will be re-created for each test class.
 * This specific test helps debug whether Spring Boot and Testcontainers are using the same connection URLs.
 * */
class UrlDuplicateConnectionTest extends IntegrationTest {
	@Test
	void testConnectionUrls() {
		// given
		String springBootUrl = getSpringBootUrl();
		String testcontainersUrl = getTestcontainersUrl();

		// then
		assertEquals(springBootUrl, testcontainersUrl);
	}
}