package org.ihtsdo.rvf.core.service.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RF2 release file name regex patterns mapped to RVF edition codes.
 * Mappings are defined in {@code application.properties} under {@code rvf.rf2-filename-edition}.
 */
@Configuration
@ConfigurationProperties(prefix = "rvf.rf2-filename-edition")
@EnableAutoConfiguration
public class Rf2FilenameEditionConfig {

	private static final int EDITION_SEPARATOR_INDEX = '=';

	private String defaultEdition = "INT";

	private List<String> patternToEdition = new ArrayList<>();

	private List<Map.Entry<Pattern, String>> compiledPatterns = List.of();

	public String getDefaultEdition() {
		return defaultEdition;
	}

	public void setDefaultEdition(String defaultEdition) {
		this.defaultEdition = defaultEdition;
	}

	public List<String> getPatternToEdition() {
		return patternToEdition;
	}

	public void setPatternToEdition(List<String> patternToEdition) {
		this.patternToEdition = patternToEdition;
		this.compiledPatterns = compilePatterns(patternToEdition);
	}

	public List<Map.Entry<Pattern, String>> getCompiledPatterns() {
		return compiledPatterns;
	}

	public String mapFilenameToEdition(String filename) {
		for (Map.Entry<Pattern, String> entry : compiledPatterns) {
			if (entry.getKey().matcher(filename).find()) {
				return entry.getValue();
			}
		}
		return defaultEdition;
	}

	private static List<Map.Entry<Pattern, String>> compilePatterns(List<String> mappings) {
		List<Map.Entry<Pattern, String>> compiled = new ArrayList<>();
		for (String mapping : mappings) {
			int separator = mapping.indexOf(EDITION_SEPARATOR_INDEX);
			if (separator <= 0 || separator == mapping.length() - 1) {
				throw new IllegalArgumentException(
						"Invalid rvf.rf2-filename-edition.pattern-to-edition entry (expected regex=edition): " + mapping);
			}
			String regex = mapping.substring(0, separator);
			String edition = mapping.substring(separator + 1);
			compiled.add(new AbstractMap.SimpleImmutableEntry<>(Pattern.compile(regex), edition));
		}
		return List.copyOf(compiled);
	}
}
