package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.logging.Level;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;

public class LoadPlayerStats extends BaseRunnable {
	private final PlayerStatsInfo pstats;
	
	public LoadPlayerStats(MainPlugin plugin, PlayerStatsInfo pstats) {
		super(plugin);
		this.pstats = pstats;
	}
	
	@Override
	public void runNow() throws Exception {
		if (plugin.getServerName() == null) {
			plugin.log(Level.INFO, "Waiting for bungeecord server name.");
			runAsync(20);
			return;
		}
		
		pstats.loadAsync();
	}
	
	protected boolean retryOnError(Exception error) {
		return error instanceof java.io.IOException;
	}
}
