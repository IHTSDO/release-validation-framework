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

    @Test
    void splitSql_ShouldRespectQuotesCommentsAndTerminators() {
        List<String> statements = AssertionsDatabaseImporter.splitSql(
                "select 1; -- comment; still comment\n" +
                "select ';'; /* block; comment */ select 2; select '''';").statements();
        assertEquals(List.of(
                "select 1",
                "-- comment; still comment\nselect ';'",
                "/* block; comment */ select 2",
                "select ''''"
        ), statements);
    }

    @Test
    void splitSql_ShouldReturnEmptyForBlankInput() {
        assertTrue(AssertionsDatabaseImporter.splitSql(null).statements().isEmpty());
        assertTrue(AssertionsDatabaseImporter.splitSql("   ").statements().isEmpty());
    }

    @Test
    void splitSql_ShouldDiscardTrailingCommentAfterFinalTerminator() {
        assertEquals(List.of("select 1"),
                AssertionsDatabaseImporter.splitSql("select 1; -- Core Module").statements());
        assertEquals(List.of("select 1"),
                AssertionsDatabaseImporter.splitSql("select 1;\n/* trailing */\n").statements());
    }

    @Test
    void splitSql_ShouldDiscardCommentOnlySegmentBetweenTerminators() {
        assertEquals(List.of("select 1", "select 2"),
                AssertionsDatabaseImporter.splitSql("select 1; /* note */ ; select 2;").statements());
    }

    @Test
    void splitSql_ShouldReportUnterminatedStatementAsTail() {
        AssertionsDatabaseImporter.SplitSql result = AssertionsDatabaseImporter.splitSql("select 1; select 2");
        assertEquals(List.of("select 1"), result.statements());
        assertEquals("select 2", result.unterminatedTail());
    }

    @Test
    void splitSql_ShouldNotReportTrailingCommentAsUnterminatedStatement() {
        assertEquals("", AssertionsDatabaseImporter.splitSql("select 1; -- Core Module").unterminatedTail());
    }
}
