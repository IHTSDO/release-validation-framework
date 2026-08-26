package org.ihtsdo.rvf.assertions;

public interface RF2 {

	interface Module {
		String CORE = "900000000000207008";
	}

	interface Refset {
		String SAME_AS = "900000000000527005";
	}

	interface Concept {
		long DEFINITION_STATUS_PRIMITIVE = 900000000000074008L;
		long DESCRIPTION_TYPE_FSN = 900000000000003001L;
		long CASE_SIGNIFICANCE_CASE_INSENSITIVE = 900000000000448009L;
		long IS_A = 116680003L;
		long CHARACTERISTIC_TYPE_STATED = 900000000000010007L;
		long MODIFIER_EXISTENTIAL = 900000000000451002L;
		long ACCEPTABILITY_PREFERRED = 900000000000548007L;
		long CORRELATION_NOT_SPECIFIED = 447561005L;
		long MAP_CATEGORY_NOT_CLASSIFIABLE = 447637006L;
		long CONTENT_ORIGIN = 900000000000460009L;
		long RULE_STRENGTH_MANDATORY = 723597001L;
		long CONTENT_TYPE_ALL = 723596005L;
		long MRCM_ATTRIBUTE_DOMAIN_ALL = 900000000000441003L;
		long MRCM_MODULE_SCOPE_RULE_REFSET = 723562003L;
	}

	interface Column {
		String TARGET_COMPONENT_ID = "targetcomponentid";
		String VALUE_ID = "valueid";
		String ACCEPTABILITY_ID = "acceptabilityid";
		String OWL_EXPRESSION = "owlexpression";
		String MAP_GROUP = "mapGroup";
		String MAP_PRIORITY = "mapPriority";
		String MAP_RULE = "mapRule";
		String MAP_ADVICE = "mapAdvice";
		String MAP_TARGET = "mapTarget";
		String CORRELATION_ID = "correlationId";
		String MAP_CATEGORY_ID = "mapCategoryId";
		String EXPRESSION = "expression";
		String DEFINITION_STATUS_ID = "definitionStatusId";
		String CONTENT_ORIGIN_ID = "contentOriginId";
		String ATTRIBUTE_ID = "attributeId";
		String DOMAIN_ID = "domainid";
		String GROUPED = "grouped";
		String ATTRIBUTE_CARDINALITY = "attributecardinality";
		String ATTRIBUTE_IN_GROUP_CARDINALITY = "attributeingroupcardinality";
		String RULE_STRENGTH_ID = "rulestrengthid";
		String CONTENT_TYPE_ID = "contenttypeid";
		String RANGE_CONSTRAINT = "rangeconstraint";
		String ATTRIBUTE_RULE = "attributerule";
		String DOMAIN_CONSTRAINT = "domainconstraint";
		String PARENT_DOMAIN = "parentdomain";
		String PROXIMAL_PRIMITIVE_CONSTRAINT = "proximalprimitiveconstraint";
		String PROXIMAL_PRIMITIVE_REFINEMENT = "proximalprimitiverefinement";
		String DOMAIN_TEMPLATE_FOR_PRECOORDINATION = "domaintemplateforprecoordination";
		String DOMAIN_TEMPLATE_FOR_POSTCOORDINATION = "domaintemplateforpostcoordination";
		String GUIDE_URL = "guideurl";
		String MRCM_RULE_REFSET_ID = "mrcmrulerefsetid";
	}
}
