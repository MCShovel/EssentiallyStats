package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.*;

import org.bukkit.command.CommandSender;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;

public class ShowPlayerStats extends BaseRunnable {
	private final CommandSender sender;
	private final String playerName, category;
	private PlayerStatsInfo _pstats;
	private final int pageNumber;

	public ShowPlayerStats(MainPlugin plugin, CommandSender sender, String playerName, String category,
			PlayerStatsInfo pstats) {
		super(plugin);
		this.sender = sender;
		this.playerName = playerName;
		this._pstats = pstats;
		int page = 1;
		if (category != null) {
			int mul = 1;
			page = 0;
			for (int ix = category.length() - 1; ix >= 0 && Character.isDigit(category.charAt(ix)); ix--) {
				page += mul * (category.charAt(ix) - '0');
				mul *= 10;
				category = category.substring(0, category.length() - 1);
			}
			category = category.trim();
		}
		this.pageNumber = Math.max(0, page-1);
		this.category = category;
	}

	public PlayerStatsInfo getPlayerInfo() throws Exception {
		try {
			if (_pstats == null) {
				PlayerNameLookup lookup = new PlayerNameLookup(plugin, playerName);
				lookup.runNow();
				if (lookup.getResults().size() == 1) {
					_pstats = lookup.getResults().get(0);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return _pstats;
	}

	@Override
	protected void runNow() throws Exception {
		PlayerStatsInfo psi = getPlayerInfo();
		if (psi == null) {
			sendMessage(sender, plugin.Config.PlayerNotFound(playerName));
			return;
		}

		String title, footer;
		StatsTable formatter;
		
		Map<String, Long> results = null;
		if (category != null) {
			StatsTable report = plugin.MySql.findTable(category);
			if (report == null) {
				sendMessage(sender, plugin.Config.get("messages.category-not-found", "&cUnable to find a category by that name."));
				return;
			}
			title = report.Category;
			footer = plugin.Config.format("messages.report-footer", "&7* Stats for user {name}&7", "name", psi.name);
			formatter = report;
			results = report.aggregateReport(psi, true, null);
		}
		else { // Default summary
			title = plugin.Config.get("messages.summary-title", "Category Summary");
			footer = plugin.Config.format("messages.summary-footer", "&7* use /stats {name} &7&o(Category)&r&7 for more detail", "name", psi.name);
			formatter = plugin.MySql.getTables().get(0);
			results = new HashMap<String, Long>();
			for (StatsTable tbl : plugin.MySql.getTables()) {
				if (tbl.canSummarize()) {
					results.put(tbl.Category, tbl.getSummaryCount(psi, null));
				}
				else {
					results.put(tbl.Category, null);
				}
			}
		}

		ArrayList<String> keys = new ArrayList<String>();
		for (String k : results.keySet())
			keys.add(k);
		keys.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			} 
		});
		
		StringBuilder sb = new StringBuilder();
		sb.append(plugin.Config.format("messages.report-header", "&6============== [&f{title}&6] ==============", 
				"title", title));
		sb.append('\n');
		int pageSize = plugin.Config.getInt("settings.page-size", 18);
		int pageCount = (keys.size() + pageSize - 1) / pageSize;
		int start = pageNumber * pageSize;
		int stop = Math.min(keys.size(), start + pageSize);
		if (pageCount > 1) {
			footer += plugin.Config.format("messages.report-page-num", " &7[Page {page} of {count}]", 
					"page", pageNumber + 1, "count", pageCount);
		}
		
		for (int ix = start; ix < stop; ix++) {
			String field = keys.get(ix);
			Long value = results.get(field);
			String formatted = value == null 
					? plugin.Config.format("messages.see-details", "(see details)")
					: formatter.formatField(field, value);
			sb.append(plugin.Config.format("messages.report-line", "&6{name}: &f{value}",
					"name", field,
					"value", formatted));
			sb.append('\n');
		}

		sb.append(footer);
		sendMessage(sender, sb.toString());
	}
}
