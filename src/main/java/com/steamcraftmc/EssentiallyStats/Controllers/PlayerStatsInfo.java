package com.steamcraftmc.EssentiallyStats.Controllers;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.tasks.LoadPlayerStats;
import com.steamcraftmc.EssentiallyStats.tasks.UpdateStatsOnJoin;

public class PlayerStatsInfo {
	private final MainPlugin plugin;
	public final UUID uniqueId;
	public final String name;
	
	private boolean hasLoaded;
	private Map<String,Long> prevStats;
	private boolean hasQuit;
	private long quitTime;

	public PlayerStatsInfo(MainPlugin plugin, Player player) {
		this.plugin = plugin;
		this.uniqueId = player.getUniqueId();
		this.name = player.getName();
		hasLoaded = false;
		prevStats = new HashMap<String,Long>();
	}

	public void Join() {
		hasQuit = false;
		if (!hasLoaded) {
			new LoadPlayerStats(plugin, this).runAsync(40);
		}
	}

	public void Quit() {
		quitTime = System.currentTimeMillis();
		hasQuit = true;
	}

	public void loadAsync() throws Exception {
		JsonStatsData stats = new JsonStatsData(plugin, uniqueId);
		if (stats.Parse()) {
			plugin.log(Level.INFO, "Loaded stats for user: " + uniqueId + ", found: " + stats.count());
			prevStats = stats.getStats();
		}

		new UpdateStatsOnJoin(plugin, this, prevStats)
			.runNow();
		
		hasLoaded = true;
	}
}
