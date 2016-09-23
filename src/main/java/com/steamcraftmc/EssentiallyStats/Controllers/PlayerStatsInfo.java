package com.steamcraftmc.EssentiallyStats.Controllers;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.tasks.LoadPlayerStats;
import com.steamcraftmc.EssentiallyStats.tasks.UpdateChangedStats;
import com.steamcraftmc.EssentiallyStats.tasks.UpdateStatsOnJoin;
import com.steamcraftmc.EssentiallyStats.utils.MyTransaction;

public class PlayerStatsInfo {
	private final MainPlugin plugin;
	public final UUID uniqueId;
	public final String name;
	
	private boolean hasLoaded;
	private Map<String,Long> prevStats;
	private boolean hasQuit;
	private long quitTime;

	public PlayerStatsInfo(MainPlugin plugin, Player player) {
		this(plugin, player.getUniqueId(), player.getName());
	}

	public PlayerStatsInfo(MainPlugin plugin, UUID uniqueId, String playerName) {
		this.plugin = plugin;
		this.uniqueId = uniqueId;
		this.name = playerName;
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
		// This will be checked later by calling hasExpired to remove players that log off
		// for longer than 5 minutes.
		quitTime = System.currentTimeMillis();
		hasQuit = true;
	}

	public boolean hasExpired() {
		// if the user has been offline for more than 5 minutes, remove them.
		if (hasQuit && (System.currentTimeMillis() - quitTime) > (1000 * 60 * 5)) {
			return true;
		}
		return false;
	}

	public void loadAsync(MyTransaction trans) throws Exception {
		// A very different update on initial join as we ensure all stats in the database are
		// at minimal set to the value currently in the json file.
		JsonStatsData stats = new JsonStatsData(plugin, uniqueId);
		if (stats.Parse()) {
			plugin.log(Level.FINE, "Loaded stats for user: " + uniqueId + ", found: " + stats.count());
			prevStats = stats.getStats();
		}

		new UpdateStatsOnJoin(plugin, this, prevStats)
			.apply(trans);
		
		hasLoaded = true;
	}
	
	public void updateAsync(MyTransaction trans) throws Exception {
		// Load the current json values...
		JsonStatsData stats = new JsonStatsData(plugin, uniqueId);
		if (stats.Parse()) {
			Map<String, Long> oldStats = prevStats; 
			Map<String, Long> newStats = stats.getStats();
			if (newStats != null && newStats.size() > 0) {
				new UpdateChangedStats(plugin, this, oldStats, newStats)
					.apply(trans);
	
				prevStats = newStats;
			}
		}
	}
}
