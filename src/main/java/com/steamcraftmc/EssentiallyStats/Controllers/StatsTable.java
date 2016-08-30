package com.steamcraftmc.EssentiallyStats.Controllers;

import java.util.*;

import org.bukkit.configuration.ConfigurationSection;

import com.steamcraftmc.EssentiallyStats.MainPlugin;

public class StatsTable {
	private final MainPlugin plugin;
	public final String TableName;
	public final String Namespace;
	private final Set<String> _knownFields;  
	
	private final boolean _addPlayerName;  
	private final boolean _isInclusive; 
	private final Set<String> _filtered;  
	
	public StatsTable(MainPlugin plugin, String name) {
		this.plugin = plugin;
		this.TableName = name;
		this._knownFields = new HashSet<String>();
		
		ConfigurationSection section = plugin.Config.getSection("tables." + TableName);
		this.Namespace = section.getString("namespace");
		this._addPlayerName = section.getBoolean("playerName", false);
		this._filtered = new HashSet<String>();

		String filter = section.getString("filter");
		if (filter != null && filter.length() > 0) {
			boolean inclusive = true;
			if (filter.charAt(0) == '!') {
				filter = filter.substring(1);
				inclusive = false;
			}
			_isInclusive = inclusive;
			for (String item : plugin.Config.getArray("filters." + filter, new String[0])) {
				_filtered.add(item);
			}
		} else {
			_isInclusive = false;
		}
	}
	
	public boolean hasPlayerName() {
		return _addPlayerName;
	}

	public void setFieldNames(List<String> fieldNames) {
		for (String f : fieldNames)
			_knownFields.add(f);
	}
}
