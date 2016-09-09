package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.CheckResults;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerRank;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.utils.MyTransaction;

public class LoadPlayerStats extends BaseRunnable {
	private final PlayerStatsInfo pstats;
	
	public LoadPlayerStats(MainPlugin plugin, PlayerStatsInfo pstats) {
		super(plugin);
		this.pstats = pstats;
	}
	
	@Override
	public void runNow() throws Exception {
		if (plugin.getServerName() == null) {
			plugin.log(Level.INFO, "Waiting for bungeecord server name.");
			runAsync(20);
			return;
		}

		MyTransaction trans = plugin.MySql.beginTransaction();
		try { pstats.loadAsync(trans); }
		finally { trans.close(); }
		
		Player player = plugin.getServer().getPlayer(pstats.uniqueId);
		if (player != null && player.isOnline()) {
			List<PlayerRank> ranks = PlayerRank.getNextRanks(plugin, player);
			if (ranks.size() == 1) {
				final CheckResults result = ranks.get(0).checkRequirements(player);
				if (result.isComplete()) {
					final LoadPlayerStats me = this;
					plugin.getServer().getScheduler()
						.runTask(plugin, new Runnable() {
							@Override public void run() { me.promotePlayer(result); }
						});
				}
			}
		}
	}
	
	protected void promotePlayer(CheckResults result) {
		try {
			Player player = plugin.getServer().getPlayer(pstats.uniqueId);
			if (player != null && player.isOnline()) {
				result.complete(player);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	protected boolean retryOnError(Exception error) {
		return error instanceof java.io.IOException;
	}
}
