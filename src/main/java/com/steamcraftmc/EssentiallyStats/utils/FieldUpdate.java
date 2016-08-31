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
		this.FieldName = cleanFieldName(name);
		this.FieldValue = value;
	}
	
	public static String cleanFieldName(String val) {
		StringBuilder sb = new StringBuilder();
		val = val.trim();
		val = val.replaceAll("\\+", "Plus");
		val = val.replaceAll("[^a-zA-Z0-9_]+", "_");
		int lengthValid = val.length();
		if (val.charAt(lengthValid - 1) == '_') {
			lengthValid--;
		}
		char prev = '_';
		for (int ix = 0; ix < lengthValid; ix++) {
			char ch = val.charAt(ix);
			if (prev != '_' && ch != '_' && Character.isLowerCase(prev) && Character.isUpperCase(ch)) {
				sb.append('_');
			}
			sb.append(Character.toLowerCase(ch));
			prev = ch;
		}
		return sb.toString();
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
