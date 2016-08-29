package com.steamcraftmc.EssentiallyStats;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WorldEvents implements Listener {
	MainPlugin plugin;

	public WorldEvents(MainPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onSignChange(SignChangeEvent event) {
	}
}
