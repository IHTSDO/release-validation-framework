package org.ihtsdo.rvf.core.service.duck;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Synthetic assertion stores in the shape {@code tools/duck-publisher} writes (formatVersion 1).
 *
 * <p>Each assertion's {@code sha256} is the publisher's: the first 16 hex characters of the
 * sha256 of its source script. Grouping files are carried verbatim, and a store given a
 * {@link Builder#pack pack identity} declares the digest its content hashes to, so it
 * verifies.
 */
final class TestStores {

	/** Groups by the three standard categories; no policies needed. */
	static final String GROUPS_XML = """
			<?xml version="1.0" encoding="UTF-8"?>
			<assertionGroupingStrategy version="1.0">
			  <group name="file-centric-validation" includeStandaloneCategories="file-centric-validation"/>
			  <group name="component-centric-validation" includeStandaloneCategories="component-centric-validation"/>
			  <group name="release-type-validation" includeStandaloneCategories="release-type-validation"/>
			</assertionGroupingStrategy>
			""";

	static final String POLICIES_XML = """
			<?xml version="1.0" encoding="UTF-8"?>
			<policyValues/>
			""";

	/** The sentinels the publisher emits, in its order. */
	private static final List<String[]> SENTINELS = List.of(
			new String[] {"<RUNID>", "424242424242424242"},
			new String[] {"<ASSERTIONUUID>", "rvfph_assertionuuid_"},
			new String[] {"<MODULEID>", "rvfph_moduleid_"},
			new String[] {"<MODULEIDS>", "rvfph_moduleids_"},
			new String[] {"<INCLUDED_MODULES>", "rvfph_included_modules_"},
			new String[] {"<VERSION>", "rvfph_version_"},
			new String[] {"<PROSPECTIVE>", "rvfph_prospective_"},
			new String[] {"<TEMP>", "rvfph_temp_"},
			new String[] {"<PREVIOUS>", "rvfph_previous_"},
			new String[] {"<DEPENDENCY>", "rvfph_dependency_"});

	private TestStores() {
	}

	static Builder builder() {
		return new Builder();
	}

	/** The publisher's per-script hash: sha256 of the text, first 16 hex characters. */
	static String sha(String source) {
		return DuckStore.sha256Hex(source).substring(0, 16);
	}

	static final class Builder {

		private final ObjectNode root = DuckStore.MAPPER.createObjectNode();
		private final ObjectNode assertions;
		private String groupsXml = GROUPS_XML;
		private String policiesXml = POLICIES_XML;
		private ObjectNode pack;

		private Builder() {
			root.put("formatVersion", DuckStore.SUPPORTED_FORMAT_VERSION);
			ObjectNode generator = root.putObject("generator");
			generator.put("sqlglot", "30.18.0");
			generator.put("python", "3.12.3");
			generator.put("publisher", "sha256:test");
			ArrayNode sentinels = root.putArray("sentinels");
			for (String[] s : SENTINELS) {
				sentinels.addObject().put("placeholder", s[0]).put("sentinel", s[1]);
			}
			root.put("runIdSentinel", "424242424242424242");
			root.put("qaResultToken", "qa_result");
			root.putArray("knownTables");
			root.putObject("tableColumns");
			root.putArray("ports");
			root.putArray("prerequisites");
			assertions = root.putObject("assertions");
		}

		/**
		 * Adds an assertion compiled from {@code source}; its statements default to the
		 * source itself.
		 */
		Builder assertion(String uuid, String file, String source, String text, String keywords,
				String... statements) {
			ObjectNode a = assertions.putObject(uuid);
			a.put("file", file);
			a.put("sha256", sha(source));
			a.put("text", text);
			a.put("keywords", keywords);
			a.put("severity", "");
			ArrayNode out = a.putArray("statements");
			for (String statement : statements.length == 0 ? new String[] {source} : statements) {
				out.add(statement);
			}
			return this;
		}

		Builder severity(String uuid, String severity) {
			((ObjectNode) assertions.get(uuid)).put("severity", severity);
			return this;
		}

		/** Declares a table, adding it to {@code knownTables} and {@code tableColumns}. */
		Builder table(String name, String columns) {
			((ArrayNode) root.get("knownTables")).add(name);
			((ObjectNode) root.get("tableColumns")).put(name, columns);
			return this;
		}

		Builder grouping(String groups, String policies) {
			this.groupsXml = groups;
			this.policiesXml = policies;
			return this;
		}

		/** A store carrying no grouping files. */
		Builder noGrouping() {
			return grouping(null, null);
		}

		/** Gives the store a pack identity whose digest is computed from its content. */
		Builder pack(String name, String version, String corpusRef) {
			pack = DuckStore.MAPPER.createObjectNode();
			pack.put("name", name);
			pack.put("version", version);
			pack.put("corpusRef", corpusRef);
			return this;
		}

		/** Sets any other top-level field ({@code ports}, {@code requires}, ...) from JSON. */
		Builder set(String field, String json) {
			root.set(field, DuckStore.MAPPER.readTree(json));
			return this;
		}

		String json() {
			ObjectNode out = root.deepCopy();
			if (groupsXml != null) {
				ArrayNode grouping = out.putArray("grouping");
				grouping.addObject().put("file", DuckAssertionSource.GROUPS_XML)
						.put("sha256", sha(groupsXml)).put("text", groupsXml);
				grouping.addObject().put("file", DuckAssertionSource.POLICIES_XML)
						.put("sha256", sha(policiesXml)).put("text", policiesXml);
			}
			if (pack != null) {
				ObjectNode identity = pack.deepCopy();
				identity.put("assertions", out.get("assertions").size());
				identity.put("digest", digestOf(out));
				out.set("pack", identity);
			}
			return out.toString();
		}

		DuckStore store() {
			try {
				return DuckStore.parse(json());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		Path write(Path file) {
			try {
				Files.createDirectories(file.toAbsolutePath().getParent());
				return Files.writeString(file, json());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private static String digestOf(JsonNode tree) {
			try {
				return DuckStore.parse(tree.toString()).assertionDigest();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}
