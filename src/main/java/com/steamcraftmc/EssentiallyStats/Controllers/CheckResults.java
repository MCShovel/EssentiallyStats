package com.steamcraftmc.EssentiallyStats.Controllers;

import java.util.*;

import org.bukkit.entity.Player;

public class CheckResults {
	
	private final PlayerStatsInfo player;
	private final PlayerRank rank;
	private final ArrayList<PartialResult> results;
	private boolean isComplete;
	
	public CheckResults(PlayerStatsInfo player, PlayerRank rank) {
		this.player = player;
		this.rank = rank;
		this.results = new ArrayList<PartialResult>();
		this.isComplete = false;
	}

	public void addResult(boolean success, long current, long target, String message) {
		isComplete = (results.size() == 0) ? success : (isComplete && success);
		results.add(new PartialResult(success, current, target, message));
	}
	
	public String getPlayerName() { return this.player.name; }
	public String getRankName() { return this.rank.Name; }
	
	public boolean isComplete() { return this.isComplete; }
	
	public List<PartialResult> getResults() {
		return Collections.unmodifiableList(this.results);
	}
	
	public class PartialResult {
		public final boolean Complete;
		public final long Current;
		public final long Target;
		public final String Message;

		public PartialResult(boolean success, long current, long target, String message) {
			this.Complete = success;
			this.Current = current;
			this.Target = target;
			this.Message = message;
		}
	}

	public void complete(Player user) {
		if (user != null && this.isComplete) {
			this.rank.execForPlayer(user);
		}
	}
}
