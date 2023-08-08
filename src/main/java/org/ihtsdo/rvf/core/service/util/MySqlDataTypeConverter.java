package org.ihtsdo.rvf.core.service.util;

import org.ihtsdo.snomed.util.rf2.schema.DataType;

import java.sql.SQLException;

public class MySqlDataTypeConverter {

	/**
	 * @param type DataType to convert
	 * @return a <code>String</code> containing the MySQL data type.
	 */
	public String convert(final DataType type) throws SQLException {
		String result = switch (type) {
            case SCTID -> "BIGINT(20)";
            case UUID -> "VARCHAR(36)";
            case BOOLEAN -> "char(1)";
            case TIME -> "char(8)";
            case INTEGER -> "INTEGER";
            case STRING -> "VARCHAR";
            default -> throw new SQLException("DataType missing from " + getClass() + " : " + type);
        };
        return result;
	}

}