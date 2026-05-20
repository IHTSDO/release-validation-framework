package org.ihtsdo.rvf.core.service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Rf2FilenameEditionConfigTest {

	private static final String MAPPING_PREFIX = "rvf.rf2-filename-edition.pattern-to-edition";

	private final Rf2FilenameEditionConfig config = new Rf2FilenameEditionConfig();

	@BeforeEach
	void loadMappingsFromApplicationProperties() throws IOException {
		Properties properties = new Properties();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
			if (in == null) {
				throw new IllegalStateException("application.properties not found on classpath");
			}
			properties.load(in);
		}
		List<String> mappings = new ArrayList<>();
		properties.stringPropertyNames().stream()
				.filter(key -> key.startsWith(MAPPING_PREFIX))
				.sorted(Comparator.comparingInt(Rf2FilenameEditionConfigTest::indexFromPropertyKey))
				.forEach(key -> mappings.add(properties.getProperty(key)));
		config.setDefaultEdition(properties.getProperty("rvf.rf2-filename-edition.default-edition", "INT"));
		config.setPatternToEdition(mappings);
	}

	private static int indexFromPropertyKey(String key) {
		int start = key.indexOf('[') + 1;
		int end = key.indexOf(']');
		return Integer.parseInt(key.substring(start, end));
	}

	@Test
	void shouldLoadAllMappingsFromApplicationProperties() {
		assertEquals(22, config.getCompiledPatterns().size());
	}

	@Test
	void shouldMapKnownRf2FilenamesToEdition() {
		assertEdition("sct2_Concept_SpanishExtensionDelta_INT_20141031.txt", "ES");
		assertEdition("sct2_Concept_SimplexEdition_Snapshot_20240131.txt", "SIMPLEX");
		assertEdition("sct2_Concept_Snapshot_NL1000146_20240131.txt", "NL");
		assertEdition("sct2_Concept_Snapshot_GB1000000_20240131.txt", "UK");
		assertEdition("sct2_Concept_Snapshot_AU1000036_20240131.txt", "AU");
		assertEdition("sct2_Concept_Snapshot_CH1000195_20240131.txt", "CH");
		assertEdition("sct2_Concept_Delta_CH100019520240131.txt", "CH");
		assertEdition("sct2_Concept_Snapshot_LO1010000_20240131.txt", "LOINC");
		assertEdition("sct2_Concept_Snapshot_INT_20240131.txt", "INT");
		assertEdition("sct2_Concept_Snapshot_TM_20240131.txt", "TM");
		assertEdition("sct2_Concept_Snapshot_XX9999999_20240131.txt", "INT");
	}

	private void assertEdition(String filename, String expectedEdition) {
		assertEquals(expectedEdition, config.mapFilenameToEdition(filename), filename);
	}
}
