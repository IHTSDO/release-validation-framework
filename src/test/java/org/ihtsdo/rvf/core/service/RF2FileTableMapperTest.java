package org.ihtsdo.rvf.core.service;

import org.ihtsdo.rvf.core.service.util.RF2FileTableMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class RF2FileTableMapperTest {
	
	private static final String[] INT_DELTA_FILES = {
			"der2_cRefset_AssociationReferenceDelta_INT_20150131.txt",
			"der2_cRefset_AttributeValueDelta_INT_20150131.txt",
			"der2_Refset_SimpleDelta_INT_20150131.txt",
			"der2_cRefset_LanguageDelta-en_INT_20150131.txt",
			"der2_iisssccRefset_ExtendedMapDelta_INT_20150131.txt",
			"der2_iissscRefset_ComplexMapDelta_INT_20150131.txt",
			"der2_sRefset_SimpleMapDelta_INT_20150131.txt",
			"sct2_Concept_Delta_INT_20150131.txt",
			"sct2_Description_Delta-en_INT_20150131.txt",
			"sct2_Relationship_Delta_INT_20150131.txt",
			"sct2_StatedRelationship_Delta_INT_20150131.txt",
			"sct2_TextDefinition_Delta-en_INT_20150131.txt",
			"xder2_sscccRefset_ExpressionAssociationDelta_INT_20150714.txt",
			"xder2_scccRefset_MapCorrelationOriginDelta_INT_20150714.txt"
			};
	
	private static final String[] SPANISH_DELTA_FILES = {
			"der2_cRefset_AssociationReferenceSpanishExtensionDelta_INT_20141031.txt",
			"der2_cRefset_AttributeValueSpanishExtensionDelta_INT_20141031.txt",
			"der2_cRefset_LanguageSpanishExtensionDelta-es_INT_20141031.txt",
			"der2_Refset_SimpleSpanishExtensionDelta_INT_20141031.txt",
			"sct2_Concept_SpanishExtensionDelta_INT_20141031.txt",
			"sct2_Description_SpanishExtensionDelta-es_INT_20141031.txt",
			"sct2_Relationship_SpanishExtensionDelta_INT_20141031.txt",
			"sct2_StatedRelationship_SpanishExtensionDelta_INT_20141031.txt",
			"sct2_TextDefinition_SpanishExtensionDelta-es_INT_20141031.txt"
	};
	
	
	private static final String[] INT_EXPECTED_DELTA = {
			"associationrefset_d",
			"attributevaluerefset_d",
			"simplerefset_d",
			"langrefset_d",
			"extendedmaprefset_d",
			"complexmaprefset_d",
			"simplemaprefset_d",
			"concept_d",
			"description_d",
			"relationship_d",
			"stated_relationship_d",
			"textdefinition_d",
			"expressionassociationrefset_d",
			"mapcorrelationoriginrefset_d"
		};
	
	private static final String[] EXTENSION_EXPECTED_DELTA = {
			"associationrefset_d",
			"attributevaluerefset_d",
			"langrefset_d",
			"simplerefset_d",
			"concept_d",
			"description_d",
			"relationship_d",
			"stated_relationship_d",
			"textdefinition_d"				
		};

	private static final String [] GPFP_DELTA_FILES = {
		"der2_cRefset_GPFPAssociationReferenceDelta_INT_20141031.txt",
		"der2_cRefset_GPFPAttributeValueDelta_INT_20141031.txt",
		"der2_cRefset_GPFPLanguageDelta-es_INT_20141031.txt",
		"der2_Refset_GPFPSimpleDelta_INT_20141031.txt",
		"sct2_Concept_GPFPDelta_INT_20141031.txt",
		"sct2_Description_GPFPDelta-es_INT_20141031.txt",
		"sct2_Relationship_GPFPDelta_INT_20141031.txt",
		"sct2_StatedRelationship_GPFPDelta_INT_20141031.txt",
		"sct2_TextDefinition_GPFPDelta-en_INT_20141031.txt"
	};

	private static final String[] GPFP_EXPECTED_DELTA = {
		"associationrefset_d",
		"attributevaluerefset_d",
		"langrefset_d",
		"simplerefset_d",
		"concept_d",
		"description_d",
		"relationship_d",
		"stated_relationship_d",
		"textdefinition_d"	
	};
	
	private static final String[] INT_PREVIEW_DELTA_FILES = {
		"xder2_cRefset_AssociationReferenceDelta_INT_20150131.txt",
		"xsct2_Relationship_Delta_INT_20150131.txt",
	};
	
	private static final String[] EXPECTED_INT_PREVIEW_DELTA = {
		"associationrefset_d",
		"relationship_d"
	};
	
	@Test
	void testInternationalDeltaFiles() {
		int i = 0;
		assertEquals(INT_DELTA_FILES.length, INT_EXPECTED_DELTA.length);
		for (final String fileName : INT_DELTA_FILES) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			assertEquals(INT_EXPECTED_DELTA[i++], tableName);
		}
	}
	
	@Test
	void testInternationalSnapshotFiles() {
		testSnapshot(INT_DELTA_FILES, INT_EXPECTED_DELTA);
	}

	
	@Test
	void testInternationalFullFiles() {
		testFull(INT_DELTA_FILES, INT_EXPECTED_DELTA);
	}


	@Test
	void testSpanishDeltaFiles() {

		assertEquals(SPANISH_DELTA_FILES.length, EXTENSION_EXPECTED_DELTA.length);
		int i=0;
		for (final String fileName : SPANISH_DELTA_FILES) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			assertEquals(EXTENSION_EXPECTED_DELTA[i++], tableName);
		}
	}
	
	@org.junit.jupiter.api.Test
	void testSpanishSnapshotFiles() {
		testSnapshot(SPANISH_DELTA_FILES, EXTENSION_EXPECTED_DELTA);
	}
	
	@Test
	void testSpanishFullFiles() {
		testFull(SPANISH_DELTA_FILES, EXTENSION_EXPECTED_DELTA);
	}
	
	@org.junit.jupiter.api.Test
	void testGPFPDeltaFiles() {
		assertEquals(GPFP_DELTA_FILES.length, GPFP_EXPECTED_DELTA.length);
		int i=0;
		for (final String fileName : GPFP_DELTA_FILES) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			assertEquals(GPFP_EXPECTED_DELTA[i++], tableName);
		}
	}
	
	@org.junit.jupiter.api.Test
	void testGPFPSnapshotFiles() {
		testSnapshot(GPFP_DELTA_FILES, GPFP_EXPECTED_DELTA);
	}
	
	@Test
	void testGPFPFullFiles() {
		testFull(GPFP_DELTA_FILES, GPFP_EXPECTED_DELTA);
	}
	
	@Test
	void testDenmarkFiles() {
		String[] dkDeltas = {"sct2_Concept_Delta_DK1000005_20160215.txt"};
		String[] expected ={"concept_d"};
		int i=0;
		for (final String fileName : dkDeltas) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			assertEquals(expected[i++], tableName);
		}
	}
	@Test
	void testTechPreviewFiles() {
		assertEquals(INT_PREVIEW_DELTA_FILES.length, EXPECTED_INT_PREVIEW_DELTA.length);
		int i=0;
		for (final String fileName : INT_PREVIEW_DELTA_FILES) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			assertEquals(EXPECTED_INT_PREVIEW_DELTA[i++], tableName);
		}
	}
	@Test
	void testSimpleMapFilenames() {
		String[] simpleMapFilenames = {
				"der2_iRefset_SimpleMapDelta_INT_20150131.txt",
				"der2_sRefset_SimpleMapDelta_INT_20150131.txt"
		};

		for (String fileName : simpleMapFilenames) {
			assertEquals("simplemaprefset_d", RF2FileTableMapper.getLegacyTableName(fileName));
		}
	}

	private void testSnapshot(final String[] deltaFiles, final String[] expectedDeltaResults) {
		final String[] snapshotFiles = new String[deltaFiles.length];
		for (int i=0; i < snapshotFiles.length; i++) {
			snapshotFiles[i] = deltaFiles[i].replace("Delta", "Snapshot");
		}
		final String[] expected = new String[expectedDeltaResults.length];
		for (int i=0;i<expected.length;i++) {
			expected[i] = expectedDeltaResults[i].replace("_d", "_s");
		}
		int i = 0;
		assertEquals(snapshotFiles.length, expected.length);
		for (final String fileName : snapshotFiles) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			assertEquals(expected[i++], tableName);
		}
	}
	
	private void testFull(final String[] deltaFiles, final String[] expectedDeltaResults) {
		final String[] snapshotFiles = new String[deltaFiles.length];
		for (int i=0; i < snapshotFiles.length; i++) {
			snapshotFiles[i] = deltaFiles[i].replace("Delta", "Full");
		}
		final String[] expected = new String[expectedDeltaResults.length];
		for (int i=0;i<expected.length;i++) {
			expected[i] = expectedDeltaResults[i].replace("_d", "_f");
		}
		int i = 0;
		assertEquals(snapshotFiles.length, expected.length);
		for (final String fileName : snapshotFiles) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			assertEquals(expected[i++], tableName);
		}
	}

}
