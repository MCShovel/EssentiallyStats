package com.steamcraftmc.EssentiallyStats.utils;

public abstract class FieldUpdate extends FieldInformation {

	protected final Object FieldValue;
	
	public FieldUpdate (String key, Object value) {
		super(key);
		this.FieldValue = value;
	}
	
	public String getFieldType() {
		if (FieldValue instanceof Long) {
			return "BIGINT";
		}
		else {
			return "VARCHAR(63)";
		}
	}
	
	public String getAssignment() {
		return String.format("%s = %s", getField(), getValue());
	}

	public String getField() {
		return String.format("`%s`", FieldName);
	}

	public String getValue() {
		if (FieldValue instanceof Number) {
			return String.valueOf(FieldValue);
		}
		else {
			return String.format("'%s'", String.valueOf(FieldValue));
		}
	}
}
