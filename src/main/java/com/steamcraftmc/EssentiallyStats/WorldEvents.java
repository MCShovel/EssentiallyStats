package com.steamcraftmc.EssentiallyStats;

import java.util.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.tasks.AsycProcessPlayers;

import org.bukkit.entity.Player;

public class WorldEvents implements Listener {
	final MainPlugin plugin;
	final HashMap<UUID, PlayerStatsInfo> players;
	private int _syncTaskId;

	public WorldEvents(MainPlugin plugin) {
		this.plugin = plugin;
		this.players = new HashMap<UUID, PlayerStatsInfo>();
		_syncTaskId = -1;
	}

	private PlayerStatsInfo getPlayer(Player player) {
		PlayerStatsInfo pstats = this.players.get(player.getUniqueId());
		if (pstats == null) {
			this.players.put(player.getUniqueId(), pstats = new PlayerStatsInfo(plugin, player));
		}
		return pstats;
	}

	public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
		final WorldEvents self = this;
		_syncTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, 
			new Runnable() {
				@Override
				public void run() { self.onSyncTimer(); }
			}, plugin.Config.getUpdateInterval(), plugin.Config.getUpdateInterval());
	}

	public void stop() {
		if (_syncTaskId != -1) {
			plugin.getServer().getScheduler().cancelTask(_syncTaskId);
			_syncTaskId = -1;
		}
    	HandlerList.unregisterAll(this);		
	}
	
	public void onSyncTimer() {
		// Make sure all players are in list...
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			getPlayer(p);
		}
		
		// Copy a list of all known players...
		ArrayList<PlayerStatsInfo> playerList = new ArrayList<PlayerStatsInfo>();
		for (PlayerStatsInfo psi : players.values()) {
			playerList.add(psi);
		}
		
		// While on game thread, remove any players offline long enough
		for (PlayerStatsInfo psi : playerList) {
			if (psi.hasExpired()) {
				players.remove(psi.uniqueId);
			}
		}
		
		new AsycProcessPlayers(plugin, Collections.unmodifiableList(playerList))
			.runAsync(10);
	}

	@EventHandler
	public void onWorldLoading(org.bukkit.event.world.WorldLoadEvent we) {
		plugin.Config.addWorldFolder(we.getWorld().getWorldFolder());
	}
	
	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		plugin.registerPlayerJoined(event.getPlayer());
		PlayerStatsInfo pstats = getPlayer(event.getPlayer());
		pstats.Join();
	}
	
	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
		PlayerStatsInfo pstats = getPlayer(event.getPlayer());
		pstats.Quit();
	}
}
