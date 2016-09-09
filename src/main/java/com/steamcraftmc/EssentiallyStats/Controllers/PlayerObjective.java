package com.steamcraftmc.EssentiallyStats.Controllers;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.utils.FieldInformation;

public class PlayerObjective {
	private final MainPlugin plugin;
	public final String Name;
	private final String server;
	private final String type;
	private final String value;
	private final String displayName;
	private final String completed;
	private final String inccomplete;
	private final StatsTable table;

	public PlayerObjective(MainPlugin plugin, String name, ConfigurationSection section) {
		this.plugin = plugin;
		this.Name = name;
	    this.server = section.getString("server", "all");
	    this.type = section.getString("type", "stat");
	    this.value = section.getString("value", "");
	    
	    if (this.type.equalsIgnoreCase("stat")) {
	    	FieldInformation fi = new FieldInformation(this.value);
	    	this.table = plugin.MySql.findTableField(fi);
		    this.displayName = fi.getDisplayName();
		    if (this.table == null) {
		    	plugin.log(Level.SEVERE, "Configuration error, can't find field " + fi.Namespace);
		    }
	    }
	    else if (this.type.equalsIgnoreCase("aggregate")) {
	    	this.table = plugin.MySql.findTable(this.value);
		    this.displayName = this.table == null ? "MISSING" : this.table.Category;
		    if (this.table == null) {
		    	plugin.log(Level.SEVERE, "Configuration error, can't find category " + this.value);
		    }
	    }
	    else {
	    	this.table = null;
	    	this.displayName = "ERROR";
	    	plugin.log(Level.SEVERE, "Configuration error, objective type " + this.type);
	    }
	    if (this.table == null) {
	    	plugin.log(Level.SEVERE, "Configuration error in objective " + Name);
	    }
	    
	    this.completed = ChatColor.translateAlternateColorCodes('&', 
	    		section.getString("completed", "&2✔ &6{name}: &f{value}&6 of &7{target}&6."))
	    		.replace("{name}", this.displayName);
	    this.inccomplete = ChatColor.translateAlternateColorCodes('&', 
	    		section.getString("inccomplete", "&4✘ &6{name}: &f{value}&6 of &f{target}&6."))
	    		.replace("{name}", this.displayName);
	}

	
	public long currentValue(Player player) {
		if (this.table == null || player == null) {
			return 0L;
		}

		PlayerStatsInfo psi = new PlayerStatsInfo(plugin, player);
		Long result = null;

		if (this.type.equalsIgnoreCase("stat")) {
	    	FieldInformation fi = new FieldInformation(this.value);
	    	Map<String, Long> set = this.table.aggregateReport(psi, false, this.server);
			result = set.get(fi.FieldName);
	    }
		else if (this.type.equalsIgnoreCase("aggregate")) {
			result = this.table.getSummaryCount(psi, this.server);
	    }
	    
		//plugin.log(Level.INFO, player.getName() + " " + this.type + " " + this.value + " = " + String.valueOf(result));
		return result == null ? 0L : result.longValue();
	}
	
	
	public String formatIncomplete(Player player, Long current, Long required) {
		String valueText = String.valueOf(current);
		String targetText = String.valueOf(required);
		
		String fieldName = this.type.equalsIgnoreCase("stat") 
				? new FieldInformation(this.value).FieldName
				: this.value;
		
		if (this.table != null && fieldName != null) {
			valueText = this.table.formatField(fieldName, current);
			targetText = this.table.formatField(fieldName, required);
		}
		
		return this.inccomplete.replace("{player}", player.getDisplayName())
				.replace("{value}", valueText)
				.replace("{target}", targetText)
				;
	}

	public String formatComplete(Player player, Long current, Long required) {
		String valueText = String.valueOf(current);
		String targetText = String.valueOf(required);
		
		String fieldName = this.type.equalsIgnoreCase("stat") 
				? new FieldInformation(this.value).FieldName
				: this.value;
		
		if (this.table != null && fieldName != null) {
			valueText = this.table.formatField(fieldName, current);
			targetText = this.table.formatField(fieldName, required);
		}
		
		return this.completed.replace("{player}", player.getDisplayName())
				.replace("{value}", valueText)
				.replace("{target}", targetText)
				;
	}

}
