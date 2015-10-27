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
    UNKNOWN("unknown");
    
    private String name;
    private TestType(String name) {
    	this.name = name;
    }
    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : name.equals(otherName);
    }

    @Override
	public String toString() {
       return this.name;
    }
}
