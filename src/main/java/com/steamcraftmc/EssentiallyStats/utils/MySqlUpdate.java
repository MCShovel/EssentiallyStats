package com.steamcraftmc.EssentiallyStats.utils;

import java.util.*;
import java.util.logging.Level;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;

public class MySqlUpdate {
	private final MainPlugin plugin;
	private final MySql sql;
	private final HashMap<String, TableUpdate> tableMap;
	
	public MySqlUpdate(MainPlugin plugin, UUID playerUUID) {
		this.plugin = plugin;
		this.sql = plugin.MySql;
		this.tableMap = new HashMap<>();
		
		for (StatsTable tbl : sql.getTables()) {
			TableUpdate updt = tableMap.get(tbl.Namespace);
			if (updt == null) {
				tableMap.put(tbl.Namespace, new TableUpdate(tbl, playerUUID));
			}
		}
	}
	
	public void updatePlayerName(String playerName) {
		for (TableUpdate updt : tableMap.values()) {
			updt.addPlayerName(playerName);
		}
	}
	
	public void add(FieldUpdate update) { 
		TableUpdate updt = tableMap.get(update.Namespace);
		if (updt != null) {
			updt.add(update);
		}
		else {
			plugin.log(Level.FINE, "Unable to find stats map for " + update.Namespace);
		}
	}

	public void exec() throws Exception {
		for (TableUpdate updt : tableMap.values()) {
			updt.exec(sql);
		}
	}
	
}
