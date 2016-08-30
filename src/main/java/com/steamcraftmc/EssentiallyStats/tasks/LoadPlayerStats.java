package com.steamcraftmc.EssentiallyStats.tasks;

import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;

public class LoadPlayerStats implements Runnable {
	private final PlayerStatsInfo pstats;
	
	public LoadPlayerStats(PlayerStatsInfo pstats) {
		this.pstats = pstats;
	}

	@Override
	public void run() {
		pstats.loadAsync();
	}
}
