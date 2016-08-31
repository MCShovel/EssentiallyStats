package com.steamcraftmc.EssentiallyStats.utils;

import java.util.*;

import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;

public class TableNamespace {
	private final UUID playerUUID;
	private final ArrayList<TableUpdate> tables;
	
	public TableNamespace(UUID playerUUID) {
		this.playerUUID = playerUUID;
		this.tables = new ArrayList<TableUpdate>();
	}

	public void addTable(StatsTable tbl) {
		tables.add(new TableUpdate(tbl, playerUUID));
	}

	public void addPlayerName(String playerName) {
		for (TableUpdate updt : tables) {
			updt.addPlayerName(playerName);
		}		
	}

	public void add(FieldUpdate update) {
		for (TableUpdate updt : tables) {
			if (updt.isMatch(update.FieldName)) {
				updt.add(update);
			}
		}
	}

	public void exec(MySql sql, MyTransaction trans) throws Exception {
		for (TableUpdate updt : tables) {
			updt.exec(sql, trans);
		}
	}
}
