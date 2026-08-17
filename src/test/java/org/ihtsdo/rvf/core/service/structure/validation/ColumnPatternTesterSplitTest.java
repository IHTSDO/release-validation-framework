package org.ihtsdo.rvf.core.service.structure.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the row-splitting contract ColumnPatternTester depends on.
 * <p>
 * This does not test the JDK for its own sake - it guards the one thing that
 * can silently regress: the {@code -1} limit in
 * {@code line.split("\t", -1)}. Without it, {@link String#split(String)}
 * discards trailing empty strings, an RF2 row with a legitimately empty last
 * column is counted one short, and ColumnCountTest reports a valid file as
 * malformed. That produced 45 false failures on AU 20260831 across the three
 * MRCMDomain files, and it is the sort of "simplification" a later reader can
 * make without noticing.
 */
class ColumnPatternTesterSplitTest {

	/**
	 * A real der2_sssssssRefset_MRCMDomainSnapshot row from AU 20260831 whose
	 * guideURL is empty. In the file the line ends 0x09 0x0D 0x0A - the tab is
	 * present, the field is present and empty, and the row has 13 columns.
	 * BufferedReader.readLine() strips the CRLF, leaving the trailing tab.
	 */
	private static final String MRCM_DOMAIN_ROW_WITH_EMPTY_GUIDE_URL = String.join("\t",
			"5b3b8cf9-0e1c-5e0e-8b4c-1c1a5d3f7a11",  // id
			"20260831",                               // effectiveTime
			"1",                                      // active
			"32506021000036107",                      // moduleId
			"723560006",                              // refsetId (MRCM domain)
			"999000011000168104",                     // referencedComponentId
			"<< 999000011000168104 |AU domain|",      // domainConstraint
			"",                                       // parentDomain
			"<< 999000011000168104",                  // proximalPrimitiveConstraint
			"",                                       // proximalPrimitiveRefinement
			"[[+id(<< 999000011000168104)]]",         // domainTemplateForPrecoordination
			"[[+scg(<< 999000011000168104)]]")        // domainTemplateForPostcoordination
			+ "\t";                                   // guideURL, empty

	private static final int RF2_MRCM_DOMAIN_COLUMNS = 13;

	@Test
	void aRowWithAnEmptyFinalColumnKeepsAllOfItsColumns() {
		assertEquals(RF2_MRCM_DOMAIN_COLUMNS,
				MRCM_DOMAIN_ROW_WITH_EMPTY_GUIDE_URL.split("\t", -1).length,
				"an RF2 row whose last column is empty still has every column - "
						+ "dropping the -1 limit is what made ColumnCountTest reject "
						+ "valid MRCMDomain rows");
	}

	@Test
	void withoutTheLimitTheFinalEmptyColumnIsLost() {
		// Documents the defect this fix removes, so the reason for the -1 is
		// visible rather than folklore.
		assertEquals(RF2_MRCM_DOMAIN_COLUMNS - 1,
				MRCM_DOMAIN_ROW_WITH_EMPTY_GUIDE_URL.split("\t").length,
				"String.split(regex) discards trailing empty strings");
	}

	@Test
	void severalTrailingEmptyColumnsAreAllKept() {
		// proximalPrimitiveRefinement, both templates and guideURL can all be
		// empty together, which would lose four columns rather than one.
		String row = String.join("\t", "id", "20260831", "1", "mod", "refset", "component",
				"domain", "parent", "prox") + "\t\t\t\t";
		assertEquals(RF2_MRCM_DOMAIN_COLUMNS, row.split("\t", -1).length);
	}

	@Test
	void aGenuinelyShortRowIsStillShort() {
		// The fix must not make malformed rows look valid.
		String missingTwoColumns = String.join("\t", "id", "20260831", "1", "mod", "refset",
				"component", "domain", "parent", "prox", "refine", "tplPre");
		assertEquals(RF2_MRCM_DOMAIN_COLUMNS - 2, missingTwoColumns.split("\t", -1).length);
	}
}
