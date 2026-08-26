package org.ihtsdo.rvf.assertions;

public interface Schema {
	interface Full {
		interface Component {
			String CONCEPT = "concept_f";
			String DESCRIPTION = "description_f";
			String TEXT_DEFINITION = "textdefinition_f";
			String RELATIONSHIP = "relationship_f";
			String STATED_RELATIONSHIP = "stated_relationship_f";
			String RELATIONSHIP_CONCRETE_VALUES = "relationship_concrete_values_f";
		}

		interface ReferenceSetMember {
			String ASSOCIATION = "associationrefset_f";
			String ATTRIBUTE_VALUE = "attributevaluerefset_f";
			String COMPLEX_MAP = "complexmaprefset_f";
			String EXTENDED_MAP = "extendedmaprefset_f";
			String EXPRESSION_ASSOCIATION = "expressionassociationrefset_f";
			String LANGUAGE = "langrefset_f";
			String MAP_CORRELATION_ORIGIN = "mapcorrelationoriginrefset_f";
			String MRCM_ATTRIBUTE_DOMAIN = "mrcmattributedomainrefset_f";
			String MRCM_ATTRIBUTE_RANGE = "mrcmattributerangerefset_f";
			String MRCM_DOMAIN = "mrcmdomainrefset_f";
			String MRCM_MODULE_SCOPE = "mrcmmodulescoperefset_f";
			String OWL_EXPRESSION = "owlexpressionrefset_f";
			String SIMPLE = "simplerefset_f";
			String SIMPLE_MAP = "simplemaprefset_f";
		}
	}

	interface Snapshot {
		interface Component {
			String CONCEPT = "concept_s";
			String DESCRIPTION = "description_s";
			String TEXT_DEFINITION = "textdefinition_s";
			String RELATIONSHIP = "relationship_s";
			String STATED_RELATIONSHIP = "stated_relationship_s";
			String RELATIONSHIP_CONCRETE_VALUES = "relationship_concrete_values_s";
		}

		interface ReferenceSetMember {
			String ASSOCIATION = "associationrefset_s";
			String ATTRIBUTE_VALUE = "attributevaluerefset_s";
			String COMPLEX_MAP = "complexmaprefset_s";
			String EXTENDED_MAP = "extendedmaprefset_s";
			String EXPRESSION_ASSOCIATION = "expressionassociationrefset_s";
			String LANGUAGE = "langrefset_s";
			String MAP_CORRELATION_ORIGIN = "mapcorrelationoriginrefset_s";
			String MRCM_ATTRIBUTE_DOMAIN = "mrcmattributedomainrefset_s";
			String MRCM_ATTRIBUTE_RANGE = "mrcmattributerangerefset_s";
			String MRCM_DOMAIN = "mrcmdomainrefset_s";
			String MRCM_MODULE_SCOPE = "mrcmmodulescoperefset_s";
			String OWL_EXPRESSION = "owlexpressionrefset_s";
			String SIMPLE = "simplerefset_s";
			String SIMPLE_MAP = "simplemaprefset_s";
		}
	}

	interface Delta {
		interface Component {
			String CONCEPT = "concept_d";
			String DESCRIPTION = "description_d";
			String TEXT_DEFINITION = "textdefinition_d";
			String RELATIONSHIP = "relationship_d";
			String STATED_RELATIONSHIP = "stated_relationship_d";
			String RELATIONSHIP_CONCRETE_VALUES = "relationship_concrete_values_d";
		}

		interface ReferenceSetMember {
			String ASSOCIATION = "associationrefset_d";
			String ATTRIBUTE_VALUE = "attributevaluerefset_d";
			String COMPLEX_MAP = "complexmaprefset_d";
			String EXTENDED_MAP = "extendedmaprefset_d";
			String EXPRESSION_ASSOCIATION = "expressionassociationrefset_d";
			String LANGUAGE = "langrefset_d";
			String MAP_CORRELATION_ORIGIN = "mapcorrelationoriginrefset_d";
			String MRCM_ATTRIBUTE_DOMAIN = "mrcmattributedomainrefset_d";
			String MRCM_ATTRIBUTE_RANGE = "mrcmattributerangerefset_d";
			String MRCM_DOMAIN = "mrcmdomainrefset_d";
			String MRCM_MODULE_SCOPE = "mrcmmodulescoperefset_d";
			String OWL_EXPRESSION = "owlexpressionrefset_d";
			String SIMPLE = "simplerefset_d";
			String SIMPLE_MAP = "simplemaprefset_d";
		}
	}
}
