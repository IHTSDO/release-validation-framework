package org.ihtsdo.rvf.execution.service.impl;

import org.junit.Assert;
import org.junit.Test;

public class RF2FileTableMapperTest {
	
	
	private static final String[] INT_DELTA_FILES = {
			"der2_cRefset_AssociationReferenceDelta_INT_20150131.txt",
			"der2_cRefset_AttributeValueDelta_INT_20150131.txt",
			"der2_Refset_SimpleDelta_INT_20150131.txt",
			"der2_cRefset_LanguageDelta-en_INT_20150131.txt",
//			"der2_iisssccRefset_ExtendedMapDelta_INT_20150131.txt",
			"der2_iissscRefset_ComplexMapDelta_INT_20150131.txt",
			"der2_sRefset_SimpleMapDelta_INT_20150131.txt",
//			"der2_cciRefset_RefsetDescriptorDelta_INT_20150131.txt",
//			"der2_ciRefset_DescriptionTypeDelta_INT_20150131.txt",
//			"der2_ssRefset_ModuleDependencyDelta_INT_20150131.txt",
			"sct2_Concept_Delta_INT_20150131.txt",
			"sct2_Description_Delta-en_INT_20150131.txt",
//			"sct2_Identifier_Delta_INT_20150131.txt",
			"sct2_Relationship_Delta_INT_20150131.txt",
			"sct2_StatedRelationship_Delta_INT_20150131.txt",
			"sct2_TextDefinition_Delta-en_INT_20150131.txt"
			};
	
	private static final String[] SPANISH_DELTA_FILES = {
			"der2_cRefset_AssociationReferenceSpanishExtensionDelta_INT_20141031.txt",
			"der2_cRefset_AttributeValueSpanishExtensionDelta_INT_20141031.txt",
			"der2_cRefset_LanguageSpanishExtensionDelta-es_INT_20141031.txt",
//			"der2_cciRefset_RefsetDescriptorSpanishExtensionDelta_INT_20141031.txt",
//			"der2_ssRefset_ModuleDependencySpanishExtensionDelta_INT_20141031.txt",
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
			"complexmaprefset_d",
			"simplemaprefset_d",
			"concept_d",
			"description_d",
			"relationship_d",
			"stated_relationship_d",
			"textdefinition_d"				
		};
	
	private static final String[] EXTENSION_EXPECTED_DELTA = {
			"associationrefset_d",
			"attributevaluerefset_d",
			"langrefset_d",
			"concept_d",
			"description_d",
			"relationship_d",
			"stated_relationship_d",
			"textdefinition_d"				
		};
	
	@Test
	public void testInternationalDeltaFiles() {
		int i = 0;
		Assert.assertTrue(INT_DELTA_FILES.length == INT_EXPECTED_DELTA.length);
		for (final String fileName : INT_DELTA_FILES) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			Assert.assertEquals(INT_EXPECTED_DELTA[i++], tableName);
		}
	}
	
	@Test
	public void testInternationalSnapshotFiles() {
		final String[] snapshotFiles = new String[INT_DELTA_FILES.length];
		for (int i=0; i < snapshotFiles.length; i++) {
			snapshotFiles[i] = INT_DELTA_FILES[i].replace("Delta", "Snapshot");
		}
		final String[] expected = new String[INT_EXPECTED_DELTA.length];
		for (int i=0;i<expected.length;i++) {
			expected[i] = INT_EXPECTED_DELTA[i].replace("_d", "_s");
		}
		int i = 0;
		Assert.assertTrue(snapshotFiles.length == expected.length);
		for (final String fileName : snapshotFiles) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			Assert.assertEquals(expected[i++], tableName);
		}
	}
	
	@Test
	public void testInternationalFullFiles() {
		final String[] snapshotFiles = new String[INT_DELTA_FILES.length];
		for (int i=0; i < snapshotFiles.length; i++) {
			snapshotFiles[i] = INT_DELTA_FILES[i].replace("Delta", "Full");
		}
		final String[] expected = new String[INT_EXPECTED_DELTA.length];
		for (int i=0;i<expected.length;i++) {
			expected[i] = INT_EXPECTED_DELTA[i].replace("_d", "_f");
		}
		int i = 0;
		Assert.assertTrue(snapshotFiles.length == expected.length);
		for (final String fileName : snapshotFiles) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			Assert.assertEquals(expected[i++], tableName);
		}
	}


	@Test
	public void testSpanishDeltaFiles() {

		Assert.assertTrue(SPANISH_DELTA_FILES.length == EXTENSION_EXPECTED_DELTA.length);
		int i=0;
		for (final String fileName : SPANISH_DELTA_FILES) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			Assert.assertEquals(EXTENSION_EXPECTED_DELTA[i++], tableName);
		}
	}
	
	@Test
	public void testSpanishSnapshotFiles() {
		final String[] snapshotFiles = new String[INT_DELTA_FILES.length];
		for (int i=0; i < snapshotFiles.length; i++) {
			snapshotFiles[i] = INT_DELTA_FILES[i].replace("Delta", "Snapshot");
		}
		final String[] expected = new String[INT_EXPECTED_DELTA.length];
		for (int i=0;i<expected.length;i++) {
			expected[i] = INT_EXPECTED_DELTA[i].replace("_d", "_s");
		}
		Assert.assertTrue(snapshotFiles.length == expected.length);
		int i=0;
		for (final String fileName : snapshotFiles) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			Assert.assertEquals(expected[i++], tableName);
		}
	}
	
	@Test
	public void testSpanishFullFiles() {
		final String[] snapshotFiles = new String[INT_DELTA_FILES.length];
		for (int i=0; i < snapshotFiles.length; i++) {
			snapshotFiles[i] = INT_DELTA_FILES[i].replace("Delta", "Full");
		}
		final String[] expected = new String[INT_EXPECTED_DELTA.length];
		for (int i=0;i<expected.length;i++) {
			expected[i] = INT_EXPECTED_DELTA[i].replace("_d", "_f");
		}
		Assert.assertTrue(snapshotFiles.length == expected.length);
		int i=0;
		for (final String fileName : snapshotFiles) {
			final String tableName = RF2FileTableMapper.getLegacyTableName(fileName);
			Assert.assertEquals(expected[i++], tableName);
		}
	}
}
