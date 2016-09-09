package com.steamcraftmc.EssentiallyStats;

import java.io.*;
import java.util.*;

import org.bukkit.configuration.InvalidConfigurationException;

import com.steamcraftmc.EssentiallyStats.Controllers.PlayerObjective;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerRank;
import com.steamcraftmc.EssentiallyStats.utils.BaseYamlSettingsFile;

public class MainConfig  extends BaseYamlSettingsFile {
	private final MainPlugin plugin;
	private String _worldFolder;
	private Map<String, PlayerRank> ranks;
	private Map<String, PlayerObjective> objectives;
	
	public MainConfig(MainPlugin plugin) { 
		super(plugin, "config.yml"); 
		this.plugin = plugin;
		this.ranks = null;
		this.objectives = null;
	}

	public String NoAccess() {
		return get("messages.no-access", "&4You do not have permission to this command.");
	}

	public String PlayerNotFound(String player) {
		return format("messages.player-not-found", "&cPlayer not found.", "player", String.valueOf(player));
	}

	public String PlayerNotOnline(String player) {
		return format("messages.player-not-found", "&cPlayer not onine.", "player", String.valueOf(player));
	}

	public String getTitle(String title) {
		return format("messages.report-header", "&6============== [&f{title}&6] ==============",
				"title", title);
	}

	public String ConfigurationError() {
		return get("messages.configuration-error", "&cConfiguration error.");
	}

	public boolean bungeeSupport() {
		return getBoolean("settings.bungeecord", false);
	}
	
	public String getBungeeServerName() throws InvalidConfigurationException {
		if (!this.bungeeSupport()) {
			return "n/a";
		}
		String serverName = getRaw("settings.serverName");
		if (serverName == null) {
			serverName = plugin.getServerName();
		}
		if (serverName == null) {
			throw new InvalidConfigurationException();
		}
		return serverName;
	}

	public void addWorldFolder(File worldFolder) {
		if (_worldFolder == null) {
			_worldFolder = getRaw("settings.worldFolder");
		}
		if (_worldFolder == null) {
			File test = new File(worldFolder, "stats");
			if (test.exists() && test.isDirectory()) {
				if (test.list().length > 0) {
					_worldFolder = worldFolder.getAbsolutePath();
				}
			}
		}
	}
	
	public String getWorldFolder() {
		return _worldFolder;
	}
	
	public int getUpdateInterval() {
		return getInt("settings.updateInterval", 6000);
	}
	
	public Map<String, PlayerObjective> getObjectives() {
		if (this.objectives == null) {
			HashMap<String, PlayerObjective> objectives = new HashMap<String, PlayerObjective>();
			for (String name : getSections("objectives")) {
				PlayerObjective obj = new PlayerObjective(this.plugin, name, getSection("objectives." + name));
				objectives.put(name.toLowerCase(), obj);
			}
			this.objectives = Collections.unmodifiableMap(objectives);
		}
		return this.objectives;
	}
	
	public Map<String, PlayerRank> getRanks() {
		if (this.ranks == null) {
			HashMap<String, PlayerRank> ranks = new HashMap<String, PlayerRank>();
			int ix = 0;
			for (String name : getSections("ranks")) {
				PlayerRank obj = new PlayerRank(this.plugin, name, ++ix, getSection("ranks." + name));
				ranks.put(name.toLowerCase(), obj);
			}
			this.ranks = Collections.unmodifiableMap(ranks);
		}
		return this.ranks;
	}
}
