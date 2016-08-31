package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.*;
import java.util.Map.Entry;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.utils.MySqlUpdate;
import com.steamcraftmc.EssentiallyStats.utils.UpdateStatValue;

public class UpdateChangedStats  extends BaseRunnable {
	private final PlayerStatsInfo player;
	private Map<String, Long> prev, stats;
	
	public UpdateChangedStats(MainPlugin plugin, PlayerStatsInfo player, Map<String, Long> prevStats, Map<String, Long> currStats) {
		super(plugin);
		this.player = player;
		this.prev = prevStats;
		this.stats = currStats;
	}
	
	@Override
	public void runNow() throws Exception {
		MySqlUpdate update = new MySqlUpdate(plugin, player.uniqueId);
		update.updatePlayerName(player.name);
		
		for (Entry<String, Long> e : stats.entrySet()) {
			
			String name =e.getKey();
			Long val = e.getValue();
			Long old = prev.get(e.getKey());
			if (old == null) old = 0L;

			if (old < val) {
				update.add(new UpdateStatValue(name, val, val - old));
			}
		}
		
		update.exec();
	}
}
