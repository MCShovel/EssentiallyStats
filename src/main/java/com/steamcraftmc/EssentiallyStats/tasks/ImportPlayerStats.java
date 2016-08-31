package com.steamcraftmc.EssentiallyStats.tasks;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.utils.NameFetcher;

public class ImportPlayerStats extends BaseRunnable {
	public final CommandSender sender;
	private ArrayList<PlayerStatsInfo> _todoList;
	private int counter, index;

	public ImportPlayerStats(MainPlugin plugin, CommandSender sender) {
		super(plugin);
		this.sender = sender;
		this._todoList = null;
		this.counter = 0;
		this.index = 0;
	}

	private String lookupUuid(UUID uuid) throws Exception {
		ArrayList<UUID> uuidList = new ArrayList<UUID>(); 
		final NameFetcher fetcher = new NameFetcher(uuidList);
		Map<UUID, String> foundPlayers = null;

		try {
			foundPlayers = fetcher.call();
		} catch (final Exception e) {
			e.printStackTrace();
			sendMessage(sender, "Unable to lookup names from mojang: " + e.getMessage());
			throw e;
		}

		if (foundPlayers != null) {
			for (final Entry<UUID, String> entry : foundPlayers.entrySet()) {
				if (entry.getKey().equals(uuid)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}
	
	@Override
	protected void runNow() throws Exception {
		if (_todoList == null) {
			_todoList = new ArrayList<PlayerStatsInfo>();
			File file = new File(plugin.Config.getWorldFolder());
			if (file.exists()) {
				file = new File(file, "stats");
				for (File f : file.listFiles()) {
					try {
						if (f.isFile() && f.getName().endsWith(".json") && f.getName().length() == 36 + 5) {
							UUID uuid = UUID.fromString(f.getName().substring(0, 36));
							String name = lookupUuid(uuid);
							PlayerStatsInfo psi = new PlayerStatsInfo(plugin, uuid, name);
							_todoList.add(psi);
						}
					}
					catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		if (this.index < _todoList.size()) {
			try {
				_todoList.get(this.index).loadAsync();
				counter ++;
			} 
			catch(Exception ex) {
				ex.printStackTrace();
			}
			
			this.index++;
			this.runAsync(10);
		}
		else {
			plugin.log(Level.INFO, "Stats imported " + counter + " file(s).");
			sendMessage(sender, plugin.Config.format("messages.import-end", "&6Import completed, {count} records imported.",
					"count", counter));
		}
	}
}
