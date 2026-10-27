package org.ihtsdo.rvf.core.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

	/**
	 * Wire format uses the enum constant name (e.g. {@code SQL}) for compatibility with existing reports.
	 */
	@JsonValue
	public String toJson() {
		return name();
	}

	/**
	 * Accepts both enum constant names ({@code SQL}) and legacy short names ({@code sql}).
	 */
	@JsonCreator
	public static TestType fromJson(String value) {
		if (value == null) {
			return null;
		}
		for (TestType type : values()) {
			if (type.name().equals(value) || type.name.equals(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown TestType: " + value);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
