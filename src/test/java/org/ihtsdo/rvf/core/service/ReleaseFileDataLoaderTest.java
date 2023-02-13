package org.ihtsdo.rvf.core.service;


import org.ihtsdo.rvf.core.service.util.MySqlDataTypeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
public class ReleaseFileDataLoaderTest {
	private ReleaseFileDataLoader loader;
	private String rf2FileName;
	@BeforeEach
	public void setUp() {
		MySqlDataTypeConverter dataConverter = new MySqlDataTypeConverter();
		loader = new ReleaseFileDataLoader(null, dataConverter);
		
	}

	@org.junit.jupiter.api.Test
	public void testLoadConceptDeltaFile() throws Exception {
		rf2FileName = "sct2_Concept_SpanishExtensionDelta_INT_20141031.txt";
		final String expected = "create table concept_d(\n" + 
				"id bigint(20) not null,\n" + 
				"effectivetime char(8) not null,\n" + 
				"active char(1) not null,\n" + 
				"moduleid bigint(20) not null,\n" + 
				"definitionstatusid bigint(20) not null,\n" + 
				"key idx_id(id),\n" + 
				"key idx_effectivetime(effectivetime),\n" + 
				"key idx_active(active),\n" + 
				"key idx_moduleid(moduleid),\n" + 
				"key idx_definitionstatusid(definitionstatusid)\n" + 
				") engine=myisam default charset=utf8;";
		
		final String script = loader.createTableSQL(rf2FileName);
		assertNotNull(script);
		assertEquals(expected, script);
	}
	
	@Test
	@Disabled
	public void testLoadTextDefinition() throws Exception {
		final String expected = "create table textdefinition_f(\n" + 
				"id bigint(20) not null,\n" + 
				"effectivetime char(8) not null,\n" + 
				"active char(1) not null,\n" + 
				"moduleid bigint(20) not null,\n" + 
				"conceptid bigint(20) not null,\n" + 
				"languagecode varchar(2) not null,\n" + 
				"typeid bigint(20) not null,\n" + 
				"term varchar(1024) not null,\n" + 
				"casesignificanceid bigint(20) not null,\n" + 
				"key idx_id(id),\n" + 
				"key idx_effectivetime(effectivetime),\n" + 
				"key idx_active(active),\n" + 
				"key idx_moduleid(moduleid),\n" + 
				"key idx_conceptid(conceptid),\n" + 
				"key idx_languagecode(languagecode),\n" + 
				"key idx_typeid(typeid),\n" + 
				"key idx_casesignificanceid(casesignificanceid)\n" + 
				") engine=myisam default charset=utf8;";
		rf2FileName = "sct2_TextDefinition_Full-en_INT_20150131.txt";
		final String script = loader.createTableSQL(rf2FileName);
		assertNotNull(script);
		assertEquals(expected, script);
	}
}
