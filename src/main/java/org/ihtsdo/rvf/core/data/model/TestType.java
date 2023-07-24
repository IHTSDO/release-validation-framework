package org.ihtsdo.rvf.core.data.model;

/**
 * An enumeration of different possible {@link Test} types.
 */
public enum TestType {
	SQL("sql"),
	REGEX("regex"),
	SEMANTIC("semantic"),
	ARCHIVE_STRUCTURAL("archiveStructural"),
	MANIFEST("manifest"),
	MRCM("mrcm"),
	TRACEABILITY("traceability"),
	DROOL_RULES("droolsRules"),
	UNKNOWN("unknown");

	private final String name;
	TestType(String name) {
		this.name = name;
	}
	public boolean equalsName(String otherName) {
		return name.equals(otherName);
	}

	@Override
	public String toString() {
	   return this.name;
	}
}
