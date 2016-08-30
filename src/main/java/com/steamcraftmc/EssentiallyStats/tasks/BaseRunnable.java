package com.steamcraftmc.EssentiallyStats.tasks;

import java.util.logging.Level;
import com.steamcraftmc.EssentiallyStats.MainPlugin;

public abstract class BaseRunnable implements Runnable {
	protected final MainPlugin plugin;
	
	public BaseRunnable(MainPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void runAsync(int delay) {
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this, delay);
	}

	protected abstract void runNow() throws Exception;

	protected boolean retryOnError(Exception error) {
		return false;
	}
	
	@Override
	public final void run() {
		try {
			runNow();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			if (plugin != null) {
				plugin.log(Level.SEVERE, "ERROR in task: " + this.toString());
				if (retryOnError(ex)) {
					plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this, 60 * 20);
				}
			}
		}
	}
}
