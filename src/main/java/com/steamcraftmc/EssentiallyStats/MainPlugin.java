package com.steamcraftmc.EssentiallyStats;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteStreams;
import com.google.common.io.ByteArrayDataInput;
import com.steamcraftmc.EssentiallyStats.tasks.RequestServerName;
import com.steamcraftmc.EssentiallyStats.utils.MySql;

public class MainPlugin extends JavaPlugin implements PluginMessageListener {
	public final   Logger  _logger;
	private WorldEvents _listener;
	public Boolean _exLogging;
	public final MainConfig Config;
	public final MySql MySql;
	private String bungeeServerName;

	public MainPlugin() {
		_exLogging = true;
		_logger = getLogger();
		_logger.setLevel(Level.ALL);
		_logger.log(Level.CONFIG, "Plugin initializing...");
		
		Config = new MainConfig(this);
		Config.load();
		this.MySql = new MySql(this); 
	}

	public void log(Level level, String text) {
		_logger.log(Level.INFO, text);
	}

    @Override
    public void onEnable() {
		if (!this.MySql.initSchema()) {
			setEnabled(false);
			return;
		}
                
    	_listener = new WorldEvents(this);
        getServer().getPluginManager().registerEvents(_listener, this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        log(Level.INFO, "Plugin listening for events.");
    }


	public String getServerName() {
		return bungeeServerName;
	}
    
    public void registerPlayerJoined(Player player) {
    	if (this.bungeeServerName == null) {
    		new RequestServerName(this, player).runAfter(20);
    	}
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();

		if (subchannel.equals("GetServer")) {
			String name = in.readUTF();
			if (name != null) {
				this.bungeeServerName = name;
				log(Level.INFO, "Received bungee server name: " + name);
			}
		}
	}
    
    @Override
    public void onDisable() {
    	HandlerList.unregisterAll(_listener);
    }

}
