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
}