package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.*;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;

public class PlayerNameLookup extends BaseRunnable {
	
	String partial;
	private List<PlayerStatsInfo> results;
	public PlayerNameLookup(MainPlugin plugin, String partial) {
		super(plugin);
		this.partial = partial;
	}
	
	@Override
	public void runNow() throws Exception {
		this.results = Collections.unmodifiableList(plugin.MySql.lookupPlayerByName(partial));
	}
	
	public List<PlayerStatsInfo> getResults() {
		return results;
	}
	
	protected boolean retryOnError(Exception error) {
		return error instanceof java.io.IOException;
	}
}
