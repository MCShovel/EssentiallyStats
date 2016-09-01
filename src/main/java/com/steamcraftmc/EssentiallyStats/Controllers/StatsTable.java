package com.steamcraftmc.EssentiallyStats.Controllers;

import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.utils.FieldUpdate;

public class StatsTable {
	private final MainPlugin plugin;
	public final String TableName;
	public final String Category;
	public final String Namespace;

	private final boolean _addPlayerName;
	private final boolean _isInclusive;
	private final Set<String> _filtered;
	private final Set<String> _timeFields;
	private final boolean _canAggregateFields;
	private final String _rowAggFunction;
	
	public StatsTable(MainPlugin plugin, String name) {
		this.plugin = plugin;
		this.TableName = name;

		ConfigurationSection section = plugin.Config.getSection("tables." + TableName);
		this.Namespace = section.getString("namespace", "estats.undefined_value");
		this.Category = section.getString("category", this.TableName);
		this._addPlayerName = section.getBoolean("playerName", false);
		this._filtered = new HashSet<String>();

		this._canAggregateFields = section.getBoolean("canAggregateFields", true);
	    this._rowAggFunction = section.getString("rowAggregate", "SUM");
	    HashSet<String> flds = new HashSet<String>();
	    for (String sfld : section.getStringList("timeFields")) {
	    	String f = FieldUpdate.cleanFieldName(sfld);
	    	flds.add(f);
	    	flds.add(FieldUpdate.toDisplayName(f));
	    }
	    this._timeFields = Collections.unmodifiableSet(flds);
		
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

	public String AggregateType() {
		return _rowAggFunction;
	}
	
	public boolean canSummarize() {
		return _canAggregateFields;
	}
	
	public boolean isTimeField(String name) {
		return _timeFields.contains(name);
	}

	public String formatField(String field, long value) {
		if (isTimeField(field)) {
			return formatTime(value);
		}
		else {
			return NumberFormat.getInstance().format(value);
		}
	}

	public static String formatTime(long timeTicks) {

		long diffInSeconds = timeTicks / 20L;
	    long min = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
	    long hours = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
	    long days = (diffInSeconds = (diffInSeconds / 24));

	    StringBuilder sb = new StringBuilder();
	    if (days > 0) {
	    	sb.append(days);
	    	sb.append(" day");
	    	if (days > 1) sb.append('s');
	    	if (hours + min > 0) sb.append(", ");
	    }
	    if (hours > 0) {
	    	sb.append(hours);
	    	sb.append(" hour");
	    	if (hours > 1) sb.append('s');
	    	if (min > 0) sb.append(" and ");
	    }
	    if (min > 0) {
	    	sb.append(min);
	    	sb.append(" minute");
	    	if (min > 1) sb.append('s');
	    }
    	return sb.toString();
	}
	
	public Long getSummaryCount(PlayerStatsInfo psi) {
		Long result = 0L;
		Map<String, Long> rs = aggregateReport(psi, false);
		for (Entry<String, Long> i : rs.entrySet()) {
			result += i.getValue();
		}
		return result;
	}
	
	public Map<String, Long> aggregateReport(PlayerStatsInfo psi, boolean useDisplayNames) {
		List<Map<String, Long>> all = plugin.MySql.fetchAllStats(psi.uniqueId, this);
		HashMap<String, Long> results = new HashMap<String, Long>();
		for (Map<String, Long> rs : all) {
			for (Entry<String, Long> i : rs.entrySet()) {
				String key = useDisplayNames ? FieldUpdate.toDisplayName(i.getKey()) : i.getKey();
				Long val = i.getValue();
				Long prev = results.get(key);
				if (prev == null) prev = 0L;
				val = (this._rowAggFunction == "MAX") ? Math.max(val, prev) : (val + prev);
				results.put(key, val);
			}
		}
		
		return Collections.unmodifiableMap(results);
	}
}
