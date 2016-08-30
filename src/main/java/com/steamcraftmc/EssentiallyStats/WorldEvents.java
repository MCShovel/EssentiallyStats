package com.steamcraftmc.EssentiallyStats;

import java.util.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;

import org.bukkit.entity.Player;

public class WorldEvents implements Listener {
	final MainPlugin plugin;
	final HashMap<UUID, PlayerStatsInfo> players;

	public WorldEvents(MainPlugin plugin) {
		this.plugin = plugin;
		this.players = new HashMap<UUID, PlayerStatsInfo>();
	}

	private PlayerStatsInfo getPlayer(Player player) {
		PlayerStatsInfo pstats = this.players.get(player.getUniqueId());
		if (pstats == null) {
			this.players.put(player.getUniqueId(), pstats = new PlayerStatsInfo(plugin, player));
		}
		return pstats;
	}

	@EventHandler
	public void onWorldLoading(org.bukkit.event.world.WorldLoadEvent we) {
		plugin.Config.addWorldFolder(we.getWorld().getWorldFolder());
	}
	
	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		PlayerStatsInfo pstats = getPlayer(event.getPlayer());
		pstats.Join();
	}
	
	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
		PlayerStatsInfo pstats = getPlayer(event.getPlayer());
		pstats.Quit();
	}
}
