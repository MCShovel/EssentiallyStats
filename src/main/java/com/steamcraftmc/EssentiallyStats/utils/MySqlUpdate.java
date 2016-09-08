package com.steamcraftmc.EssentiallyStats.utils;

import java.util.*;
import java.util.logging.Level;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;

public class MySqlUpdate {
	private final MainPlugin plugin;
	private final MySql sql;
	private final HashMap<String, TableNamespace> tableMap;
	
	public MySqlUpdate(MainPlugin plugin, UUID playerUUID) {
		this.plugin = plugin;
		this.sql = plugin.MySql;
		this.tableMap = new HashMap<>();
		
		for (StatsTable tbl : sql.getTables()) {
			TableNamespace updt = tableMap.get(tbl.Namespace);
			if (updt == null) {
				tableMap.put(tbl.Namespace, updt = new TableNamespace(playerUUID));
			}

			updt.addTable(tbl);
		}
	}
	
	public void updatePlayerName(String playerName) {
		for (TableNamespace updt : tableMap.values()) {
			updt.addPlayerName(playerName);
		}
	}
	
	public void add(FieldUpdate update) { 
		TableNamespace updt = tableMap.get(update.Namespace);
		if (updt != null) {
			updt.add(update);
		}
		else {
			if (!plugin.Config.getBoolean("ignore." + update.Namespace, false))
				plugin.log(Level.FINE, "Unable to find stats map for " + update.Namespace);
		}
	}

	public void exec(MyTransaction trans) throws Exception {
		try {
			for (TableNamespace updt : tableMap.values()) {
				updt.exec(sql, trans);
			}
			trans.commit();
		}
		catch(Exception ex) {
			trans.rollback();
			throw ex;
		}
	}
}
