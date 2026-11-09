package org.ihtsdo.rvf.core.service.duck;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * Small synthetic stores and packs, shaped like the published store: sentinels, tables,
 * ports, prerequisites, grouping files and optionally a pack identity.
 */
final class PackFixtures {

	static final String GROUPS_XML = """
			<assertionGroupingStrategy>
			 <group name="component-centric-validation" includeStandaloneCategories="component-centric-validation"/>
			</assertionGroupingStrategy>
			""";
	static final String POLICIES_XML = "<policyValues/>\n";

	static final String RUN_ID = "424242424242424242";

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private PackFixtures() {
	}

	static Builder store() {
		return new Builder();
	}

	/** The body a test writes for a script, and its 16-char hash as the publisher records it. */
	static String sourceOf(String file) {
		return "-- " + file + "\ninsert into qa_result select 1;\n";
	}

	static String sourceHash(String file) {
		return sha256Hex(sourceOf(file)).substring(0, 16);
	}

	static String sha256Hex(String text) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static final class Builder {

		private final ObjectNode root = MAPPER.createObjectNode();
		private final ObjectNode assertions;
		private final ArrayNode ports;
		private final ArrayNode prerequisites;
		private final ObjectNode tableColumns;
		private final ArrayNode knownTables;
		private ArrayNode grouping;
		private String[] identity;

		private Builder() {
			root.put("formatVersion", 1);
			ObjectNode generator = root.putObject("generator");
			generator.put("sqlglot", "30.18.0");
			generator.put("python", "3.12.3");
			generator.put("publisher", "sha256:test");
			root.put("runIdSentinel", RUN_ID);
			root.put("qaResultToken", "qa_result");
			ArrayNode sentinels = root.putArray("sentinels");
			sentinel(sentinels, "<RUNID>", RUN_ID);
			sentinel(sentinels, "<ASSERTIONUUID>", "rvfph_assertionuuid_");
			sentinel(sentinels, "<PROSPECTIVE>", "rvfph_prospective_");
			sentinel(sentinels, "<PREVIOUS>", "rvfph_previous_");
			knownTables = root.putArray("knownTables");
			tableColumns = root.putObject("tableColumns");
			ports = root.putArray("ports");
			prerequisites = root.putArray("prerequisites");
			grouping(GROUPS_XML, POLICIES_XML);
			assertions = root.putObject("assertions");
		}

		private static void sentinel(ArrayNode sentinels, String placeholder, String sentinel) {
			ObjectNode s = sentinels.addObject();
			s.put("placeholder", placeholder);
			s.put("sentinel", sentinel);
		}

		Builder table(String name, String columns) {
			knownTables.add(name);
			tableColumns.put(name, columns);
			return this;
		}

		Builder port(String statement) {
			ports.add(statement);
			return this;
		}

		Builder prerequisite(String file, String sha256, String statement) {
			ObjectNode p = prerequisites.addObject();
			p.put("file", file);
			p.put("sha256", sha256);
			p.putArray("statements").add(statement);
			return this;
		}

		/** One assertion whose recorded hash is {@link #sourceHash} of its file. */
		Builder assertion(String uuid, String file, String keywords, String statement) {
			return assertion(uuid, file, sourceHash(file), keywords, statement);
		}

		Builder assertion(String uuid, String file, String sha256, String keywords, String statement) {
			ObjectNode a = assertions.putObject(uuid);
			a.put("file", file);
			a.put("sha256", sha256);
			a.put("text", file);
			a.put("keywords", keywords);
			a.put("severity", "");
			a.putArray("statements").add(statement);
			return this;
		}

		Builder grouping(String groupsXml, String policiesXml) {
			grouping = root.putArray("grouping");
			for (String[] f : List.of(new String[] {"groups.xml", groupsXml},
					new String[] {"policies.xml", policiesXml})) {
				ObjectNode g = grouping.addObject();
				g.put("file", f[0]);
				g.put("sha256", sha256Hex(f[1]));
				g.put("text", f[1]);
			}
			return this;
		}

		Builder noGrouping() {
			root.remove("grouping");
			grouping = null;
			return this;
		}

		Builder transpiler(String version) {
			((ObjectNode) root.get("generator")).put("sqlglot", version);
			return this;
		}

		Builder requires(String requirementJson) {
			ArrayNode requires = root.has("requires") ? (ArrayNode) root.get("requires") : root.putArray("requires");
			requires.add(MAPPER.readTree(requirementJson));
			return this;
		}

		/** A pack identity whose digest is computed here, independently of {@link DuckStore}. */
		Builder identity(String name, String version, String corpusRef) {
			identity = new String[] {name, version, corpusRef};
			return this;
		}

		String json() {
			ObjectNode out = root.deepCopy();
			if (identity != null) {
				ObjectNode pack = out.putObject("pack");
				pack.put("name", identity[0]);
				pack.put("version", identity[1]);
				pack.put("corpusRef", identity[2]);
				pack.put("digest", digest());
				pack.put("assertions", assertions.size());
			}
			return out.toString();
		}

		DuckStore parse() throws IOException {
			return DuckStore.parse(json());
		}

		/**
		 * The publisher's digest: sorted uuid/hash/keywords/severity/text lines, then
		 * prerequisite and grouping file/hash lines.
		 */
		String digest() {
			List<String> lines = new ArrayList<>();
			for (String uuid : assertions.propertyNames()) {
				JsonNode a = assertions.get(uuid);
				lines.add(uuid + "\t" + a.get("sha256").asString("") + "\t" + a.get("keywords").asString("")
						+ "\t" + a.get("severity").asString("") + "\t" + a.get("text").asString(""));
			}
			Collections.sort(lines);
			return "sha256:" + sha256Hex(String.join("\n", lines)
					+ "\n--\n" + fileLines(prerequisites)
					+ "\n--\n" + fileLines(grouping));
		}

		private static String fileLines(ArrayNode files) {
			List<String> lines = new ArrayList<>();
			if (files != null) {
				files.forEach(f -> lines.add(f.get("file").asString("") + "\t" + f.get("sha256").asString("")));
			}
			Collections.sort(lines);
			return String.join("\n", lines);
		}
	}
}
