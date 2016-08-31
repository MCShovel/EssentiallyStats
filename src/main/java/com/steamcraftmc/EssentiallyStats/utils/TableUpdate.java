package com.steamcraftmc.EssentiallyStats.utils;

import java.util.*;

import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;

public class TableUpdate {
	private final StatsTable table;
	private final UUID playerUUID;
	private final ArrayList<FieldUpdate> updates;
	private boolean _forceUpdate;
	
	public TableUpdate(StatsTable table, UUID playerUUID) {
		this.table = table;
		this.playerUUID = playerUUID;
		this.updates = new ArrayList<FieldUpdate>();
		this._forceUpdate = false;
	}

	public void addPlayerName(String playerName) {
		this._forceUpdate = true;
		if (table.hasPlayerName()) {
			updates.add(new PlayerNameUpdate(playerName));
		}
	}

	public boolean isMatch(String fieldName) {
		return table.isMatch(fieldName);
	}
	
	public boolean hasUpdates() {
		return _forceUpdate || updates.size() > 0;
	}
	
	public void add(FieldUpdate update) {
		updates.add(update);
	}

	public void exec(MySql sql, MyTransaction trans) throws Exception {
		if (!hasUpdates()) {
			return;
		}
		
		sql.update(trans, table, playerUUID, updates);
	}
}


