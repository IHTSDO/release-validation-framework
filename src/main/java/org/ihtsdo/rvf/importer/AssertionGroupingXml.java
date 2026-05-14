package org.ihtsdo.rvf.importer;

/**
 * XML local names for {@code assertionGroupingStrategy} and {@code group} elements. Keeps
 * {@link AssertionGroupImporter} free of string literals for these names.
 */
final class AssertionGroupingXml {

	static final String STRATEGY_ELEMENT = "assertionGroupingStrategy";
	static final String GROUP_ELEMENT = "group";
	static final String POLICY_VALUES_ELEMENT = "policyValues";
	static final String POLICY_ELEMENT = "policy";
	static final String ATTR_NAME = "name";
	static final String ATTR_ASSERTION_UUIDS = "assertionUuids";
	static final String ATTR_INCLUDE_CATEGORY_WITH_CENTRE = "includeCategoryWithCentre";
	static final String ATTR_EXCLUDE_CATEGORIES = "excludeCategories";
	static final String ATTR_INCLUDE_STANDALONE_CATEGORIES = "includeStandaloneCategories";
	static final String ATTR_ASSERTION_KEYWORDS = "excludeAssertionKeywords";
	static final String ATTR_EXCLUDE_BY_POLICY = "excludeByPolicy";
	static final String ATTR_INCLUDE_BY_POLICY = "includeByPolicy";


	private AssertionGroupingXml() {
	}
}
