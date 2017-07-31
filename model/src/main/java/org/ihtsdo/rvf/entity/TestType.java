package org.ihtsdo.rvf.entity;

/**
 * An enumeration of different possible {@link org.ihtsdo.rvf.entity.Test} types.
 */
public enum TestType {
    SQL("sql"), 
    REGEX("regex"),
    SEMANTIC("semantic"), 
    ARCHIVE_STRUCTURAL("archiveStructural"),
    MANIFEST("manifest"),
    MRCM("mrcm"),
    DROOL_RULES("droolsRules"),
    UNKNOWN("unknown");
    
    private String name;
    TestType(String name) {
    	this.name = name;
    }
    public boolean equalsName(String otherName) {
        return otherName != null && name.equals(otherName);
    }

    @Override
	public String toString() {
       return this.name;
    }
}
