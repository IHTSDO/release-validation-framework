package org.ihtsdo.rvf.importer;

import org.jdom2.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssertionsDatabaseImporterTest {

    private AssertionsDatabaseImporter importer;

    // set up the importer
    @BeforeEach
    void setUp() {
        importer = new AssertionsDatabaseImporter();
    }


    @Test
    void testGetScriptElements() throws IOException {
        // load src/test/resources/assertions_manifest.xml as stream from resource
        InputStream inputStream = getClass().getResourceAsStream("/assertions_manifest.xml");
        List<Element> elements = importer.getScriptElements(inputStream);
        assertNotNull(elements, "script elements should not be empty");
        assertEquals(2, elements.size());
        ArrayList<String> expectedUuids = new ArrayList<>();
        for (Element element : elements) {
            assertNotNull(element.getAttributeValue("uuid"));
            expectedUuids.add(element.getAttributeValue("uuid"));
            assertNotNull(element.getAttributeValue("category"));
            assertNotNull(element.getAttributeValue("sqlFile"));
            assertNotNull(element.getAttributeValue("text"));
        }
        assertTrue(expectedUuids.contains("84f5edda-1249-4d79-87da-e248e61f06a6"));
        assertTrue(expectedUuids.contains("1be975bb-2a1b-4c21-ae61-6e9fcd556718"));
    }

    /**
     * A token with a RAT schema prefix still gets its schema and its release
     * type suffix. These are the mappings the assertions rely on, so they are
     * pinned first - the fix must not reach them.
     */
    @Test
    void prefixedTokensStillMapToASchemaQualifiedTable() {
        assertEquals("<PROSPECTIVE>.relationship_<SNAPSHOT>",
                importer.getRvfSchemaMapping("curr_relationship_s").get("curr_relationship_s"));
        assertEquals("<PREVIOUS>.concept_<FULL>",
                importer.getRvfSchemaMapping("prev_concept_f").get("prev_concept_f"));
        assertEquals("<DEPENDENCY>.description_<DELTA>",
                importer.getRvfSchemaMapping("dependency_description_d").get("dependency_description_d"));
    }

    /**
     * The defect: an unprefixed token ending _s/_d/_f was rewritten to a
     * qualifier with an empty schema - ".relationship_&lt;SNAPSHOT&gt;" - which is
     * a syntax error wherever it is substituted. Three assertions have failed to
     * execute on every nightly since at least May 2025 because of it.
     * <p>
     * These tokens must be left alone. Unqualified is already correct: the
     * execution connection sets its default catalog to the prospective schema.
     */
    @Test
    void unprefixedTokensAreLeftAlone() {
        for (String token : new String[]{
                "relationship_s",                 // findAncestors
                "stated_relationship_s",          // findDescendants
                "expressionassociationrefset_s",  // LOINC expression validation
                "concept_d",
                "description_f"}) {
            assertTrue(importer.getRvfSchemaMapping(token).isEmpty(),
                    token + " named no schema, so it must not be rewritten - it used to "
                            + "become '." + token + "', which cannot parse");
        }
    }

    /**
     * A quoted table name that whitespace-splitting broke apart keeps its
     * leading quote, because the caller only strips quotes when BOTH ends are
     * quoted. Such a fragment does not start with "curr_", so no schema is
     * recognised - and it used to be rewritten to
     * ".'curr_stated_relationship_&lt;SNAPSHOT&gt;", which is the reported
     * failure of "The current stated relationship snapshot file is an accurate
     * derivative...".
     */
    @Test
    void quoteFragmentsAreLeftAlone() {
        assertTrue(importer.getRvfSchemaMapping("'curr_stated_relationship_s").isEmpty(),
                "a fragment starting with a quote names no schema this method can see");
    }

    /**
     * Tokens that are not table references at all reach this method too: the
     * caller splits every statement on whitespace and passes each token in.
     */
    @Test
    void nonTableTokensProduceNoMapping() {
        for (String token : new String[]{"SELECT", "FROM", "WHERE", "1", "active", ""}) {
            assertTrue(importer.getRvfSchemaMapping(token).isEmpty(),
                    "'" + token + "' is not a table reference");
        }
    }

    /**
     * v_ tokens name a temp table and carry their own qualifier. The suffix
     * block must not append a second one on top.
     */
    @Test
    void tempTableTokensKeepASingleQualifier() {
        assertEquals("<TEMP>.attributedescription_s",
                importer.getRvfSchemaMapping("v_attributedescription_s").get("v_attributedescription_s"));
    }
}