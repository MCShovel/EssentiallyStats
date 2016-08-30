package com.steamcraftmc.EssentiallyStats.utils;

public abstract class FieldUpdate {

	protected final String Namespace;
	protected final String FieldName;
	protected final Object FieldValue;
	
	public FieldUpdate (String key, Object value) {
		String ns = "";
		String name = key;
		int ixLastDot = key.lastIndexOf('.');
		if (ixLastDot > 0) {
			ns = key.substring(0, ixLastDot);
			name = key.substring(ixLastDot + 1);
		}
		
		this.Namespace = ns;
		this.FieldName = cleanName(name);
		this.FieldValue = value;
	}
	
	private static String cleanName(String name) {
		return name.replaceAll("[^a-zA-Z0-9_]+", "_").trim().toLowerCase();
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
