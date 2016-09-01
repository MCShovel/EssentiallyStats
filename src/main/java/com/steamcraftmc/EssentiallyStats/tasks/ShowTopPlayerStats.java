package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.*;
import org.bukkit.command.CommandSender;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;
import com.steamcraftmc.EssentiallyStats.utils.FieldUpdate;
import com.steamcraftmc.EssentiallyStats.utils.NameValuePair;

public class ShowTopPlayerStats extends BaseRunnable {

	private final CommandSender sender;
	private final String field;
	
	public ShowTopPlayerStats(MainPlugin plugin, CommandSender sender, String field) {
		super(plugin);
		this.sender = sender;
		this.field = field;
	}

	@Override
	protected void runNow() throws Exception {
		try {
			String report = generateReport();
			if (report != null){
				sendMessage(sender, report);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
			sendMessage(sender, plugin.Config.get("messages.internal-error", "&cAn error occurred while processing this request."));
		}
	}

	protected String generateReport() throws Exception {
		StatsTable tbl = plugin.MySql.findTable(field);
		if (tbl == null) {
			sendMessage(sender, plugin.Config.get("messages.category-not-found", "&cUnable to find a category by that name."));
			return null;
		}
		String fieldName = plugin.MySql.findTableField(tbl, field.substring(tbl.Category.length()).trim());
		if (fieldName == null) {
			sendMessage(sender, plugin.Config.format("messages.field-not-found", "&cUnable to find a field called '{name}'.", "name", field));
			return null;
		}
		
		List<NameValuePair<Long>> results = plugin.MySql.getTop(tbl, fieldName, plugin.Config.getInt("settings.top", 10));
		
		String title = "Top " + FieldUpdate.toDisplayName(fieldName);
		StringBuilder sb = new StringBuilder();
		sb.append(plugin.Config.format("messages.report-header", "&6============== [&f{title}&6] ==============", 
				"title", title));
		sb.append('\n');
		
		for (NameValuePair<Long> pair : results) {
			String field = pair.Name;
			Long value = pair.Value;
			String formatted = tbl.formatField(field, value);
			sb.append(plugin.Config.format("messages.report-line", "&6{name}: &f{value}",
					"name", field,
					"value", formatted));
			sb.append('\n');
		}
		return sb.toString();
	}

}
