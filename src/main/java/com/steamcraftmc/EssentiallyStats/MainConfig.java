package com.steamcraftmc.EssentiallyStats;

import java.io.*;

import com.steamcraftmc.EssentiallyStats.utils.BaseYamlSettingsFile;

public class MainConfig  extends BaseYamlSettingsFile {
	private String _worldFolder;
	
	public MainConfig(MainPlugin plugin) { super(plugin, "config.yml"); }

	public String NoAccess() {
		return get("messages.no-access", "&4You do not have permission to this command.");
	}

	public String PlayerNotFound(String player) {
		return format("message.player-not-found", "&cPlayer not found.", "player", String.valueOf(player));
	}

	public boolean bungeeSupport() {
		return getBoolean("settings.useBungeeCord", false);
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
}
