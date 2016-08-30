package com.steamcraftmc.EssentiallyStats.Controllers;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.tasks.LoadPlayerStats;

public class PlayerStatsInfo {
	private final MainPlugin plugin;
	public final UUID uniqueId;
	public final String name;
	
	private boolean hasLoaded;
	private boolean hasQuit;
	private long quitTime;

	public PlayerStatsInfo(MainPlugin plugin, Player player) {
		this.plugin = plugin;
		this.uniqueId = player.getUniqueId();
		this.name = player.getName();
		hasLoaded = false;
	}

	public void Join() {
		hasQuit = false;
		if (!hasLoaded) {
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new LoadPlayerStats(this));
		}
	}

	public void Quit() {
		quitTime = System.currentTimeMillis();
		hasQuit = true;
	}

	public void loadAsync() {
		JsonStatsData stats = new JsonStatsData(plugin, uniqueId);
		plugin.log(Level.INFO, "Loaded stats for user: " + uniqueId + ", found: " + stats.count());
	}
}
