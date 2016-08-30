package com.steamcraftmc.EssentiallyStats;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.steamcraftmc.EssentiallyStats.utils.MySql;

public class MainPlugin extends JavaPlugin {
	public final   Logger  _logger;
	private WorldEvents _listener;
	public Boolean _exLogging;
	public final MainConfig Config;
	public final MySql MySql;

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
        log(Level.INFO, "Plugin listening for events.");
    }

    @Override
    public void onDisable() {
    	HandlerList.unregisterAll(_listener);
    }

}
