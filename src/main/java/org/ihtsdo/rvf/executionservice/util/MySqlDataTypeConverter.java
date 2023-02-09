package org.ihtsdo.rvf.executionservice.util;

import org.ihtsdo.snomed.util.rf2.schema.DataType;

import java.sql.SQLException;

public class MySqlDataTypeConverter {

	/**
	 * @param type DataType to convert
	 * @return a <code>String</code> containing the MySQL data type.
	 */
	public String convert(final DataType type) throws SQLException {
		String result;
		switch (type) {
			case SCTID:
				result = "BIGINT(20)";
				break;
			case UUID:
				result = "VARCHAR(36)";
				break;
			case BOOLEAN:
				result = "char(1)";
				break;
			case TIME:
				result = "char(8)";
				break;
			case INTEGER:
				result = "INTEGER";
				break;
			case STRING:
				result = "VARCHAR";
				break;
			default:
				throw new SQLException("DataType missing from " + getClass() + " : " + type);
		}
		return result;
	}

}