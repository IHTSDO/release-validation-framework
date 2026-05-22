package org.ihtsdo.rvf.importer;

/**
 * XML local names for {@code assertionGroupingStrategy} and {@code group} elements. Keeps
 * {@link AssertionGroupImporter} free of string literals for these names.
 */
final class AssertionGroupingXml {

	/** Group membership rules (alongside {@code manifest.xml}). */
	static final String GROUPS_RESOURCE_FILENAME = "groups.xml";

	/** Named assertion policies referenced from {@code groups.xml}. */
	static final String POLICIES_RESOURCE_FILENAME = "policies.xml";

	static final String STRATEGY_ELEMENT = "assertionGroupingStrategy";
	static final String GROUP_ELEMENT = "group";
	static final String POLICY_VALUES_ELEMENT = "policyValues";
	static final String POLICY_ELEMENT = "policy";
	static final String ATTR_NAME = "name";
	static final String ATTR_ASSERTION_UUIDS = "assertionUuids";
	static final String ATTR_ASSERTION_TEXT_PHRASES = "assertionTextPhrases";
	static final String ATTR_CATEGORY_WITH_CENTRE_PAIRS = "categoryWithCentrePairs";
	static final String ATTR_EXCLUDE_CATEGORIES = "excludeCategories";
	static final String ATTR_INCLUDE_STANDALONE_CATEGORIES = "includeStandaloneCategories";
	static final String ATTR_EXCLUDE_BY_POLICY = "excludeByPolicy";
	static final String ATTR_INCLUDE_BY_POLICY = "includeByPolicy";


	private AssertionGroupingXml() {
	}
}
