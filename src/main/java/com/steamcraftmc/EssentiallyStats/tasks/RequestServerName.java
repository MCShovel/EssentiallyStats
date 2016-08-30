package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.steamcraftmc.EssentiallyStats.MainPlugin;


public class RequestServerName implements Runnable {
	private final MainPlugin plugin;
	private final Player player;
	public RequestServerName(MainPlugin plugin, Player player) {
		this.plugin = plugin;
		this.player = player;
	}
	
	@Override
	public void run() {
		if (!player.isOnline()) return;
		plugin.log(Level.INFO, "Sending message: GetServer");
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}
	
	public void runAfter(int serverTicks) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, serverTicks);
	}
}
