package com.steamcraftmc.EssentiallyStats.utils;

public class FieldInformation {
	public final String Namespace;
	public final String FieldName;
	
	public FieldInformation (String key) {
		String ns = "";
		String name = key;
		int ixLastDot = key.lastIndexOf('.');
		if (ixLastDot > 0) {
			ns = key.substring(0, ixLastDot);
			name = key.substring(ixLastDot + 1);
		}
		
		this.Namespace = ns;
		this.FieldName = cleanFieldName(name);
	}
	
	public String getDisplayName() {
		return toDisplayName(FieldName);
	}

	public static String cleanFieldName(String val) {
		StringBuilder sb = new StringBuilder();
		val = val.trim();
		val = val.replaceAll("\\+", "Plus");
		val = val.replaceAll("[^a-zA-Z0-9_]+", "_");
		int lengthValid = val.length();
		if (lengthValid > 0 && val.charAt(lengthValid - 1) == '_') {
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


	public static String toDisplayName(String key) {
		StringBuilder sb = new StringBuilder();
		for (String n : key.split("_")) {
			if (n.length() > 0) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(Character.toUpperCase(n.charAt(0)));
				sb.append(n.substring(1));
			}
		}
		return sb.toString();
	}
	
}
