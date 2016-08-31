package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.List;
import java.util.logging.Level;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.utils.MyTransaction;

public class AsycProcessPlayers extends BaseRunnable {

	final List<PlayerStatsInfo> players;
	
	public AsycProcessPlayers(MainPlugin plugin, List<PlayerStatsInfo> list) {
		super(plugin);
		players = list;
	}

	@Override
	protected void runNow() throws Exception {
		MyTransaction trans = plugin.MySql.beginTransaction();
		try { 
			for (PlayerStatsInfo psi : players) {
				try {
					psi.updateAsync(trans);
				}
				catch(Exception ex) {
					plugin.log(Level.SEVERE, "Failed to update stats for " + psi.uniqueId);
					ex.printStackTrace();
				}
			}
		}
		finally { 
			trans.close(); 
		}
	}
}
