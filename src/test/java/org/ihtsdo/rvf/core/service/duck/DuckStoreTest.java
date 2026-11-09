package org.ihtsdo.rvf.core.service.duck;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckStoreTest {

	private static final String STORE = """
			{
			 "formatVersion": 1,
			 "generator": {"sqlglot": "30.18.0", "python": "3.12.3", "publisher": "sha256:abc"},
			 "sentinels": [
			  {"placeholder": "<RUNID>", "sentinel": "424242424242424242"},
			  {"placeholder": "<MODULEID>", "sentinel": "rvfph_moduleid_"},
			  {"placeholder": "<MODULEIDS>", "sentinel": "rvfph_moduleids_"}
			 ],
			 "tableColumns": {"concept_s": "id BIGINT, active VARCHAR"},
			 "ports": ["CREATE OR REPLACE MACRO m(x) AS x"],
			 "prerequisites": [
			  {"file": "pre-requisites.sql", "sha256": "0123456789abcdef",
			   "statements": ["create table a as select 1", "create table b as select 2"]}
			 ],
			 "grouping": [
			  {"file": "groups.xml", "sha256": "1111111111111111", "text": "<assertionGroupingStrategy/>"},
			  {"file": "policies.xml", "sha256": "2222222222222222", "text": "<policyValues/>"}
			 ],
			 "assertions": {
			  "aaa-1": {"file": "one.sql", "sha256": "aaaaaaaaaaaaaaaa", "text": "Assertion one",
			            "keywords": "component-centric", "severity": "WARNING",
			            "statements": ["select 1", "select 2"]},
			  "bbb-2": {"file": "two.sql", "statements": ["select 3"]}
			 }
			}
			""";

	@Test
	void readsAssertionsWithTheirMetadata() throws IOException {
		DuckStore store = DuckStore.parse(STORE);
		DuckStore.StoredAssertion a = store.assertions().get("aaa-1");
		assertEquals("one.sql", a.file());
		assertEquals("aaaaaaaaaaaaaaaa", a.sha256());
		assertEquals("Assertion one", a.text());
		assertEquals("component-centric", a.keywords());
		assertEquals("WARNING", a.severity());
		assertEquals(2, a.statements().size());
	}

	@Test
	void absentMetadataReadsAsEmptyNotNull() throws IOException {
		// Group rules match on text and keywords; a null there would be an NPE inside
		// group resolution rather than a miss.
		DuckStore.StoredAssertion b = DuckStore.parse(STORE).assertions().get("bbb-2");
		assertEquals("", b.text());
		assertEquals("", b.keywords());
		assertEquals("", b.severity());
		assertEquals("", b.sha256());
	}

	@Test
	void sentinelOrderIsPreserved() throws IOException {
		// <MODULEID>'s sentinel is a prefix of <MODULEIDS>'s but for the trailing
		// terminator, so the binder must apply them in the publisher's order.
		assertEquals("[<RUNID>, <MODULEID>, <MODULEIDS>]",
				DuckStore.parse(STORE).sentinels().keySet().toString());
	}

	@Test
	void prerequisiteStatementsAreFlattenedInOrder() throws IOException {
		assertEquals("[create table a as select 1, create table b as select 2]",
				DuckStore.parse(STORE).prerequisiteStatements().toString());
	}

	@Test
	void portsAndTableColumnsAreRead() throws IOException {
		DuckStore store = DuckStore.parse(STORE);
		assertEquals(1, store.ports().size());
		assertEquals("id BIGINT, active VARCHAR", store.tableColumns().get("concept_s"));
	}

	@Test
	void groupingFilesAreCarriedVerbatim() throws IOException {
		DuckStore.GroupingFile groups = DuckStore.parse(STORE).grouping().get("groups.xml");
		assertEquals("1111111111111111", groups.sha256());
		assertEquals("<assertionGroupingStrategy/>", groups.text());
		assertEquals(List.of("groups.xml", "policies.xml"),
				List.copyOf(DuckStore.parse(STORE).grouping().keySet()));
	}

	@Test
	void generatorProvenanceIsCarried() throws IOException {
		DuckStore store = DuckStore.parse(STORE);
		assertEquals("30.18.0", store.transpilerVersion());
		assertEquals("sha256:abc", store.publisherIdentity());
		assertTrue(store.generatorDescription().contains("30.18.0"));
	}

	@Test
	void aStoreWithoutAPackIdentityHasNone() throws IOException {
		DuckStore store = DuckStore.parse(STORE);
		assertTrue(store.identity().isEmpty());
		assertEquals("", store.corpusRef());
		assertEquals(List.of(), store.provenance());
	}

	@Test
	void aPublishedIdentityVerifiesAndIsTheProvenance() throws IOException {
		DuckStore store = TestStores.builder()
				.assertion("11111111-1111-1111-1111-111111111111", "a.sql", "select 1;", "A",
						"file-centric-validation")
				.pack("international", "2026.10.13", "c3a6fb27b2193925be7bce94ec1c4b4b03c8764f")
				.store();
		DuckStore.PackRecord identity = store.identity().orElseThrow();
		assertEquals(new DuckStore.PackRecord("international", "2026.10.13", store.assertionDigest(),
				"c3a6fb27b2193925be7bce94ec1c4b4b03c8764f", 1), identity);
		assertEquals(List.of(identity), store.provenance());
	}

	@Test
	void anEditedStoreCannotClaimThePublishedIdentity() throws IOException {
		// The digest is recomputed, so a report never records an identity the
		// executed content does not have.
		String json = TestStores.builder()
				.assertion("11111111-1111-1111-1111-111111111111", "a.sql", "select 1;", "A",
						"file-centric-validation")
				.pack("international", "1", "")
				.json();
		DuckStore edited = DuckStore.parse(json.replace(TestStores.sha("select 1;"), TestStores.sha("select 2;")));
		IOException e = assertThrows(IOException.class, edited::identity);
		assertTrue(e.getMessage().contains("has been edited"), e.getMessage());
	}

	@Test
	void theDigestCoversAssertionsTheirMetadataPrerequisitesAndGrouping() throws IOException {
		String digest = DuckStore.parse(STORE).assertionDigest();
		assertTrue(digest.matches("sha256:[0-9a-f]{64}"), digest);
		assertNotEquals(digest, DuckStore.parse(STORE.replace("aaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb"))
				.assertionDigest(), "an assertion's source hash");
		// Keywords and text decide group membership, severity failure or warning, and the
		// text is what a report says.
		assertNotEquals(digest, DuckStore.parse(STORE.replace("Assertion one", "Assertion 1"))
				.assertionDigest(), "an assertion's text");
		assertNotEquals(digest, DuckStore.parse(STORE.replace("\"component-centric\"", "\"file-centric\""))
				.assertionDigest(), "an assertion's keywords");
		assertNotEquals(digest, DuckStore.parse(STORE.replace("\"WARNING\"", "\"ERROR\""))
				.assertionDigest(), "an assertion's severity");
		assertNotEquals(digest, DuckStore.parse(STORE.replace("0123456789abcdef", "fedcba9876543210"))
				.assertionDigest(), "a prerequisite's hash");
		assertNotEquals(digest, DuckStore.parse(STORE.replace("1111111111111111", "3333333333333333"))
				.assertionDigest(), "a grouping file's hash");
	}

	@Test
	void theDigestIgnoresStatementsAndAssertionOrder() throws IOException {
		// Over identities, so a republish with another transpiler keeps the digest.
		String digest = DuckStore.parse(STORE).assertionDigest();
		assertEquals(digest, DuckStore.parse(STORE.replace("\"select 1\"", "\"SELECT 1\""))
				.assertionDigest());
		String a = "11111111-1111-1111-1111-111111111111";
		String b = "22222222-2222-2222-2222-222222222222";
		assertEquals(
				TestStores.builder().assertion(a, "a.sql", "select 1;", "A", "k")
						.assertion(b, "b.sql", "select 2;", "B", "k").store().assertionDigest(),
				TestStores.builder().assertion(b, "b.sql", "select 2;", "B", "k")
						.assertion(a, "a.sql", "select 1;", "A", "k").store().assertionDigest());
	}

	@Test
	void anUnsupportedFormatVersionIsRefused() {
		// Every section is read by name with a default, so a store of another shape
		// would read as empty, and an empty corpus reports no findings.
		IOException e = assertThrows(IOException.class,
				() -> DuckStore.parse(STORE.replace("\"formatVersion\": 1", "\"formatVersion\": 2")));
		assertTrue(e.getMessage().contains("formatVersion 2"));
	}

	@Test
	void aStoreWithNoVersionAtAllIsRefused() {
		assertThrows(IOException.class, () -> DuckStore.parse("{\"assertions\": {}}"));
	}

	@Test
	void invalidJsonIsAnIOException() {
		assertThrows(IOException.class, () -> DuckStore.parse("{not json"));
	}

	@Test
	void aColumnListCarryingSqlIsRefused() {
		// tableColumns reaches DDL and read_csv before a run's connection is locked down.
		IOException e = assertThrows(IOException.class, () -> DuckStore.parse(STORE.replace(
				"\"id BIGINT, active VARCHAR\"",
				"\"id BIGINT); COPY (SELECT 1) TO '/tmp/x.csv'; CREATE TABLE z (a INT\"")));
		assertTrue(e.getMessage().contains("tableColumns declares concept_s"), e.getMessage());
	}

	@Test
	void aTableNameThatIsNotAPlainIdentifierIsRefused() {
		IOException e = assertThrows(IOException.class, () -> DuckStore.parse(STORE.replace(
				"\"concept_s\": \"id", "\"concept_s (a INT); COPY x TO 'y'; CREATE TABLE t\": \"id")));
		assertTrue(e.getMessage().contains("not a plain identifier"), e.getMessage());
	}

	@Test
	void anUnknownColumnTypeIsRefusedAndASizedOneAccepted() throws IOException {
		assertThrows(IOException.class, () -> DuckStore.parse(STORE.replace("active VARCHAR", "active BLOB")));
		assertThrows(IOException.class, () -> DuckStore.parse(STORE.replace("active VARCHAR", "active DECIMAL(10,2)")));
		assertEquals("id BIGINT, active VARCHAR(1)",
				DuckStore.parse(STORE.replace("active VARCHAR", "active VARCHAR(1)")).tableColumns().get("concept_s"));
	}
}
