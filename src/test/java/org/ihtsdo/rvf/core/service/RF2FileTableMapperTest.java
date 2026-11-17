package org.ihtsdo.rvf.core.service;

import org.ihtsdo.rvf.core.service.util.RF2FileTableMapper;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void testAnnotationRefsetFilenames() {
		for (String type : new String[]{"Delta", "Snapshot", "Full"}) {
			String suffix = "_" + Character.toLowerCase(type.charAt(0));
			assertEquals("componentannotationstringvaluerefset" + suffix, RF2FileTableMapper.getLegacyTableName(
					"der2_scsRefset_ComponentAnnotationStringValue" + type + "_INT_20231201.txt"));
			assertEquals("memberannotationstringvaluerefset" + suffix, RF2FileTableMapper.getLegacyTableName(
					"der2_sscsRefset_MemberAnnotationStringValue" + type + "_INT_20231201.txt"));
		}
		assertEquals("componentannotationstringvaluerefset_s", RF2FileTableMapper.getLegacyTableName(
				"der2_scsRefset_OrphanetComponentAnnotationStringValueSnapshot_INT_20240131.txt"));
		assertEquals("memberannotationstringvaluerefset_d", RF2FileTableMapper.getLegacyTableName(
				"der2_sscsRefset_MemberAnnotationStringValueDelta_IE1000220_20240421.txt"));
		assertEquals("componentannotationstringvaluerefset_s", RF2FileTableMapper.getLegacyTableName(
				"xder2_scsRefset_ComponentAnnotationStringValueSnapshot_INT_20231201.txt"));
	}

	@Test
	void testAnnotationRefsetSchemaMatchesLoadedColumns() throws FileRecognitionException, IOException {
		SchemaFactory schemaFactory = new SchemaFactory();
		TableSchema component = schemaFactory.createSchemaBean(
				"der2_scsRefset_ComponentAnnotationStringValueSnapshot_INT_20231201.txt");
		TableSchema member = schemaFactory.createSchemaBean(
				"der2_sscsRefset_MemberAnnotationStringValueSnapshot_INT_20231201.txt");
		assertNotNull(component);
		assertNotNull(member);
		assertEquals(List.of(DataType.STRING, DataType.SCTID, DataType.STRING), additionalFieldTypes(component));
		assertEquals(List.of(DataType.STRING, DataType.STRING, DataType.SCTID, DataType.STRING), additionalFieldTypes(member));

		String ddl;
		try (InputStream in = RF2FileTableMapper.class.getResourceAsStream("/sql/create-tables-mysql.sql")) {
			assertNotNull(in);
			ddl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		List<String> componentColumns = List.of(
				"id", "effectivetime", "active", "moduleid", "refsetid", "referencedcomponentid",
				"languagedialectcode", "typeid", "value");
		List<String> memberColumns = List.of(
				"id", "effectivetime", "active", "moduleid", "refsetid", "referencedcomponentid",
				"referencedmemberid", "languagedialectcode", "typeid", "value");
		assertEquals(component.getFields().size(), componentColumns.size());
		assertEquals(member.getFields().size(), memberColumns.size());
		for (String suffix : List.of("_f", "_s", "_d")) {
			assertEquals(componentColumns, columnNames(ddl, "componentannotationstringvaluerefset" + suffix));
			assertEquals(memberColumns, columnNames(ddl, "memberannotationstringvaluerefset" + suffix));
		}
	}

	private static List<DataType> additionalFieldTypes(TableSchema schema) {
		List<DataType> types = new ArrayList<>();
		for (int i = 6; i < schema.getFields().size(); i++) {
			types.add(schema.getFields().get(i).getType());
		}
		return types;
	}

	private static List<String> columnNames(String ddl, String table) {
		int start = ddl.indexOf("create table " + table + "(");
		assertTrue(start >= 0, table);
		int end = ddl.indexOf("\n    key ", start);
		assertTrue(end > start, table);
		String body = ddl.substring(ddl.indexOf('(', start) + 1, end);
		List<String> names = new ArrayList<>();
		for (String line : body.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			names.add(trimmed.substring(0, trimmed.indexOf(' ')));
		}
		return names;
	}

	@Test
	void testIdentifierExtendedAssociationAndAttributeValueMapFilenames() {
		for (String type : new String[]{"Delta", "Snapshot", "Full"}) {
			String suffix = "_" + Character.toLowerCase(type.charAt(0));
			assertEquals("identifier" + suffix, RF2FileTableMapper.getLegacyTableName(
					"sct2_Identifier_" + type + "_INT_20250101.txt"));
			assertEquals("extendedassociation" + suffix, RF2FileTableMapper.getLegacyTableName(
					"der2_ccRefset_ExtendedAssociation" + type + "_INT_20250101.txt"));
			assertEquals("attributevaluemap" + suffix, RF2FileTableMapper.getLegacyTableName(
					"der2_csRefset_AttributeValueMap" + type + "_INT_20250101.txt"));
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
