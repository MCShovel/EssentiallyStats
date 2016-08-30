package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.*;
import java.util.Map.Entry;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.utils.ApplyMinValue;
import com.steamcraftmc.EssentiallyStats.utils.MySqlUpdate;

public class UpdateStatsOnJoin  extends BaseRunnable {
	private final PlayerStatsInfo player;
	private Map<String, Long> stats;
	
	public UpdateStatsOnJoin(MainPlugin plugin, PlayerStatsInfo player, Map<String, Long> prevStats) {
		super(plugin);
		this.player = player;
		this.stats = prevStats;
	}
	
	@Override
	public void runNow() throws Exception {
		MySqlUpdate update = new MySqlUpdate(plugin, player.uniqueId);
		update.updatePlayerName(player.name);
		
		for (Entry<String, Long> e : stats.entrySet()) {
			update.add(new ApplyMinValue(e.getKey(), e.getValue()));
		}
		
		update.exec();
	}
}
