package com.steamcraftmc.EssentiallyStats.Controllers;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.json.JSONArray;
import com.steamcraftmc.EssentiallyStats.json.JSONObject;

public class JsonStatsData {
	private final MainPlugin plugin;
	private final UUID uuid;
	private final HashMap<String, Long> stats;
	private final boolean loaded;

	public JsonStatsData(MainPlugin plugin, UUID playerUUID) {
		this.plugin = plugin;
		this.uuid = playerUUID;
		this.stats = new HashMap<String, Long>();

		boolean loaded;
		try {
			loaded = Parse();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			loaded = false;
		}
		this.loaded = loaded;
	}
	
	private void debug(String message) {
		plugin.log(Level.INFO, message);
	}

	public int count() {
		return loaded ? -1 : stats.size();
	}

	private boolean Parse() throws IOException {
		File file = new File(plugin.Config.getWorldFolder());
		if (file.exists()) {
			File jsonFile = new File(new File(file, "stats"), String.valueOf(uuid) + ".json");
			if (jsonFile.exists() && jsonFile.isFile()) {
				String contents = readTextFile(jsonFile);
				loadJsonData(contents);
			}
		}

		return false;
	}

	private String readTextFile(File file) throws IOException {
		String str;
        StringBuilder sb = new StringBuilder();
        try (FileInputStream ins = new FileInputStream(file)) {
        	BufferedReader in = new BufferedReader(new InputStreamReader(ins, "UTF8"));
        	while ((str = in.readLine()) != null) {
	        	sb.append(str);
	        }
        in.close();
        }
        return sb.toString();
    }

	private void loadJsonData(String contents) {
		JSONObject json = new JSONObject(contents);
		loadJsonData("", json);
	}

	private void loadJsonData(String namespace, JSONObject json) {
		JSONArray fields = json.names();
		for (int ix = 0; ix < fields.length(); ix++) {
			String key = fields.getString(ix);
			Object val = json.get(key);
			addJsonValue(namespace + key, val);
		}
	}
	
	private void addJsonValue(String key, Object val) {
		if (val != null && val instanceof Number) {
			debug("Found stat " + key + " = " + String.valueOf(val));
			stats.put(key, ((Number)val).longValue());
		}
		else if (val != null && val instanceof Boolean) {
			debug("Found stat " + key + " = " + String.valueOf(val));
			stats.put(key, val.equals(Boolean.TRUE) ? 1L : 0L);
		}
		else if (val != null && val instanceof String) {
			key = key + "." + nameToField((String)val);
			val = 1;
			debug("Found stat " + key + " = " + String.valueOf(val));
			stats.put(key, 1L);
		}
		else if (val != null && val instanceof JSONArray) {
			debug("Found stat array in " + key);
			JSONArray array = (JSONArray)val;
			for (int ix = 0; ix < array.length(); ix++) {
				addJsonValue(key, array.get(ix));
			}
		}
		else if (val != null && val instanceof JSONObject) {
			debug("Found stat object in " + key);
			loadJsonData(key + ".", (JSONObject)val);
		}
		else {
			debug("Unsupported value type: " + String.valueOf(val));
		}
	}

	private String nameToField(String val) {
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
}
