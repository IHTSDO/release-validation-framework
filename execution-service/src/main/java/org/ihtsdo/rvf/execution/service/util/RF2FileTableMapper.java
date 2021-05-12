package org.ihtsdo.rvf.execution.service.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RF2FileTableMapper {

	private static final String TEXT_DEFINITION_FILE_HEADER = "sct2_TextDefinition_";
	private static final String STATED_RELATIONSHIP_FILE_HEADER = "sct2_StatedRelationship_";
	private static final String SIMPLE_FILE_HEADER = "der2_Refset_.*Simple";
	private static final String SIMPLE_MAP_FILE_HEADER = "der2_sRefset_.*SimpleMap";
	private static final String RELATIONSHIP_FILE_HEADER = "sct2_Relationship_";
	private static final String RELATIONSHIP_CONCRETE_VALUES_FILE_HEADER = "sct2_RelationshipConcreteValues";
	private static final String LANGUAGE_FILE_HEADER = "der2_cRefset_.*Language";
	private static final String COMPLEX_MAP_FILE_HEADER = "der2_iissscRefset_.*ComplexMap";
	private static final String EXTENDED_MAP_FILE_HEADER = "der2_iisssccRefset_.*ExtendedMap";
	private static final String ATTRIBUTE_VALUE_FILE_HEADER = "der2_cRefset_.*AttributeValue";
	private static final String ASSOCIATION_REFERENCE_FILE_HEADER = "der2_cRefset_.*Association";
	private static final String DESCRIPTION_FILE_HEADER = "sct2_Description_";
	private static final String CONCEPT_FILE_HEADER = "sct2_Concept_";
	private static final String EXPRESSION_ASSOCIATION_FILE_HEADER = "der2_sscccRefset_.*ExpressionAssociation";
	private static final String MAP_CORRELATION_ORIGIN_FILE_HEADER = "der2_scccRefset_.*MapCorrelationOrigin";
	private static final String MODULE_DEPENDENCY_FILE_HEADER = "der2_ssRefset_ModuleDependency";
	private static final String REFSET_DESCRIPTOR_FILE_HEADER = "der2_cciRefset_RefsetDescriptor";
	private static final String OWL_EXPRESSION_FILE_HEADER = "sct2_sRefset_OWLExpression";
	private static final String MRCM_ATTRIBUTE_DOMAIN_FILE_HEADER = "der2_cissccRefset_MRCMAttributeDomain";
	private static final String MRCM_MODULE_SCOPE_FILE_HEADER = "der2_cRefset_.*MRCMModuleScope";
	private static final String MRCM_ATRRIBUTE_RANGE_FILE_HEADER = "der2_ssccRefset_.*MRCMAttributeRange";
	private static final String MRCM_DOMAIN_FILE_HEADER = "der2_sssssssRefset_.*MRCMDomain";
	private static final String DESCRIPTION_TYPE_FILE_HEADER = "der2_ciRefset_.*DescriptionType";
	private static final Map<String,String> tableNameMap = new HashMap<>();
	private static final String DELTA = ".*Delta.*_*_\\d{8}.txt";
	private static final String SNAPSHOT = ".*Snapshot.*_*_\\d{8}.txt";
	private static final String FULL = ".*Full.*_*_\\d{8}.txt";
	//list of file name regex expressions with RVF table names
	static {
		//Delta
		tableNameMap.put(CONCEPT_FILE_HEADER + DELTA, "concept_d");
		tableNameMap.put(DESCRIPTION_FILE_HEADER + DELTA, "description_d");
		tableNameMap.put(ASSOCIATION_REFERENCE_FILE_HEADER+ DELTA, "associationrefset_d");
		tableNameMap.put(ATTRIBUTE_VALUE_FILE_HEADER + DELTA, "attributevaluerefset_d");
		tableNameMap.put(COMPLEX_MAP_FILE_HEADER + DELTA, "complexmaprefset_d");
		tableNameMap.put(EXTENDED_MAP_FILE_HEADER + DELTA, "extendedmaprefset_d");
		tableNameMap.put(LANGUAGE_FILE_HEADER + DELTA, "langrefset_d");
		tableNameMap.put(RELATIONSHIP_FILE_HEADER + DELTA, "relationship_d");
		tableNameMap.put(RELATIONSHIP_CONCRETE_VALUES_FILE_HEADER + DELTA, "relationship_concrete_values_d");
		tableNameMap.put(SIMPLE_MAP_FILE_HEADER + DELTA, "simplemaprefset_d");
		tableNameMap.put(SIMPLE_FILE_HEADER + DELTA, "simplerefset_d");
		tableNameMap.put(STATED_RELATIONSHIP_FILE_HEADER + DELTA, "stated_relationship_d");
		tableNameMap.put(TEXT_DEFINITION_FILE_HEADER + DELTA, "textdefinition_d");
		tableNameMap.put(EXPRESSION_ASSOCIATION_FILE_HEADER + DELTA, "expressionAssociationRefset_d");
		tableNameMap.put(MAP_CORRELATION_ORIGIN_FILE_HEADER + DELTA, "mapCorrelationOriginRefset_d");
		tableNameMap.put(MODULE_DEPENDENCY_FILE_HEADER + DELTA, "moduledependencyrefset_d");
		tableNameMap.put(REFSET_DESCRIPTOR_FILE_HEADER + DELTA, "refsetdescriptor_d");
		tableNameMap.put(OWL_EXPRESSION_FILE_HEADER + DELTA, "owlexpressionrefset_d");
		tableNameMap.put(MRCM_ATTRIBUTE_DOMAIN_FILE_HEADER + DELTA, "mrcmattributedomainrefset_d");
		tableNameMap.put(MRCM_MODULE_SCOPE_FILE_HEADER + DELTA, "mrcmmodulescoperefset_d");
		tableNameMap.put(MRCM_ATRRIBUTE_RANGE_FILE_HEADER + DELTA, "mrcmattributerangerefset_d");
		tableNameMap.put(MRCM_DOMAIN_FILE_HEADER + DELTA, "mrcmdomainrefset_d");
		tableNameMap.put(DESCRIPTION_TYPE_FILE_HEADER + DELTA, "descriptiontyperefset_d");
		
		//Full
		tableNameMap.put(CONCEPT_FILE_HEADER + FULL, "concept_f");
		tableNameMap.put(DESCRIPTION_FILE_HEADER + FULL, "description_f");
		tableNameMap.put(ASSOCIATION_REFERENCE_FILE_HEADER+ FULL, "associationrefset_f");
		tableNameMap.put(ATTRIBUTE_VALUE_FILE_HEADER + FULL, "attributevaluerefset_f");
		tableNameMap.put(COMPLEX_MAP_FILE_HEADER + FULL, "complexmaprefset_f");
		tableNameMap.put(EXTENDED_MAP_FILE_HEADER + FULL, "extendedmaprefset_f");
		tableNameMap.put(LANGUAGE_FILE_HEADER + FULL, "langrefset_f");
		tableNameMap.put(RELATIONSHIP_FILE_HEADER + FULL, "relationship_f");
		tableNameMap.put(RELATIONSHIP_CONCRETE_VALUES_FILE_HEADER + FULL, "relationship_concrete_values_f");
		tableNameMap.put(SIMPLE_MAP_FILE_HEADER + FULL, "simplemaprefset_f");
		tableNameMap.put(SIMPLE_FILE_HEADER + FULL, "simplerefset_f");
		tableNameMap.put(STATED_RELATIONSHIP_FILE_HEADER + FULL, "stated_relationship_f");
		tableNameMap.put(TEXT_DEFINITION_FILE_HEADER + FULL, "textdefinition_f");
		tableNameMap.put(EXPRESSION_ASSOCIATION_FILE_HEADER + FULL, "expressionAssociationRefset_f");
		tableNameMap.put(MAP_CORRELATION_ORIGIN_FILE_HEADER + FULL, "mapCorrelationOriginRefset_f");
		tableNameMap.put(MODULE_DEPENDENCY_FILE_HEADER + FULL, "moduledependencyrefset_f");
		tableNameMap.put(REFSET_DESCRIPTOR_FILE_HEADER + FULL, "refsetdescriptor_f");
		tableNameMap.put(OWL_EXPRESSION_FILE_HEADER + FULL, "owlexpressionrefset_f");
		tableNameMap.put(MRCM_ATTRIBUTE_DOMAIN_FILE_HEADER + FULL, "mrcmattributedomainrefset_f");
		tableNameMap.put(MRCM_MODULE_SCOPE_FILE_HEADER + FULL, "mrcmmodulescoperefset_f");
		tableNameMap.put(MRCM_ATRRIBUTE_RANGE_FILE_HEADER + FULL, "mrcmattributerangerefset_f");
		tableNameMap.put(MRCM_DOMAIN_FILE_HEADER + FULL, "mrcmdomainrefset_f");
		tableNameMap.put(DESCRIPTION_TYPE_FILE_HEADER + FULL, "descriptiontyperefset_f");
		
		//Snapshot
		tableNameMap.put(CONCEPT_FILE_HEADER + SNAPSHOT, "concept_s");
		tableNameMap.put(DESCRIPTION_FILE_HEADER + SNAPSHOT, "description_s");
		tableNameMap.put(ASSOCIATION_REFERENCE_FILE_HEADER+ SNAPSHOT, "associationrefset_s");
		tableNameMap.put(ATTRIBUTE_VALUE_FILE_HEADER + SNAPSHOT, "attributevaluerefset_s");
		tableNameMap.put(COMPLEX_MAP_FILE_HEADER + SNAPSHOT, "complexmaprefset_s");
		tableNameMap.put(EXTENDED_MAP_FILE_HEADER + SNAPSHOT, "extendedmaprefset_s");
		tableNameMap.put(LANGUAGE_FILE_HEADER + SNAPSHOT, "langrefset_s");
		tableNameMap.put(RELATIONSHIP_FILE_HEADER + SNAPSHOT, "relationship_s");
		tableNameMap.put(RELATIONSHIP_CONCRETE_VALUES_FILE_HEADER + SNAPSHOT, "relationship_concrete_values_s");
		tableNameMap.put(SIMPLE_MAP_FILE_HEADER + SNAPSHOT, "simplemaprefset_s");
		tableNameMap.put(SIMPLE_FILE_HEADER + SNAPSHOT, "simplerefset_s");
		tableNameMap.put(STATED_RELATIONSHIP_FILE_HEADER + SNAPSHOT, "stated_relationship_s");
		tableNameMap.put(TEXT_DEFINITION_FILE_HEADER + SNAPSHOT, "textdefinition_s");
		tableNameMap.put(EXPRESSION_ASSOCIATION_FILE_HEADER + SNAPSHOT, "expressionAssociationRefset_s");
		tableNameMap.put(MAP_CORRELATION_ORIGIN_FILE_HEADER + SNAPSHOT, "mapCorrelationOriginRefset_s");
		tableNameMap.put(MODULE_DEPENDENCY_FILE_HEADER + SNAPSHOT, "moduledependencyrefset_s");
		tableNameMap.put(REFSET_DESCRIPTOR_FILE_HEADER + SNAPSHOT, "refsetdescriptor_s");
		tableNameMap.put(OWL_EXPRESSION_FILE_HEADER + SNAPSHOT, "owlexpressionrefset_s");
		tableNameMap.put(MRCM_ATTRIBUTE_DOMAIN_FILE_HEADER + SNAPSHOT, "mrcmattributedomainrefset_s");
		tableNameMap.put(MRCM_MODULE_SCOPE_FILE_HEADER + SNAPSHOT, "mrcmmodulescoperefset_s");
		tableNameMap.put(MRCM_ATRRIBUTE_RANGE_FILE_HEADER + SNAPSHOT, "mrcmattributerangerefset_s");
		tableNameMap.put(MRCM_DOMAIN_FILE_HEADER + SNAPSHOT, "mrcmdomainrefset_s");
		tableNameMap.put(DESCRIPTION_TYPE_FILE_HEADER + SNAPSHOT, "descriptiontyperefset_s");
	}
	
	public static String getLegacyTableName(final String filename) {
		final String fileName = filename.startsWith("x") ? filename.substring(1) : filename;
		for(final String regex : tableNameMap.keySet()) {
			if (Pattern.compile(regex).matcher(fileName).matches()) {
				return tableNameMap.get(regex);
			}
		}
		return null;
	}
	
	public static Collection<String> getAllTableNames() {
		return tableNameMap.values();
	}
}
