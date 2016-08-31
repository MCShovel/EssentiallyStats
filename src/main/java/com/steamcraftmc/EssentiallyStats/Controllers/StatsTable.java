package com.steamcraftmc.EssentiallyStats.Controllers;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.utils.FieldUpdate;

public class StatsTable {
	private final MainPlugin plugin;
	public final String TableName;
	public final String Namespace;

	private final boolean _addPlayerName;
	private final boolean _isInclusive;
	private final Set<String> _filtered;

	public StatsTable(MainPlugin plugin, String name) {
		this.plugin = plugin;
		this.TableName = name;

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
				_filtered.add(FieldUpdate.cleanFieldName(item));
			}
		} else {
			_isInclusive = false;
		}

		plugin.log(Level.FINE,
				String.format("Table %s using ns=%s, pname=%s, filt=%s, size=%d", this.TableName, this.Namespace,
						this._addPlayerName ? "yes" : "no", this._isInclusive ? "yes" : "no", this._filtered.size()));
	}

	public boolean isMatch(String fieldName) {
		boolean match = _filtered.contains(fieldName);
		if (_isInclusive == false) {
			match = !match;
		}
		return match;
	}

	public boolean hasPlayerName() {
		return _addPlayerName;
	}
}
