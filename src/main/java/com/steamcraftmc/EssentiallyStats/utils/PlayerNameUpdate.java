package com.steamcraftmc.EssentiallyStats.utils;

public class PlayerNameUpdate extends FieldUpdate {

	public PlayerNameUpdate(String playerName) {
		super("playerName", cleanName(playerName));
	}
	
	static String cleanName(String name) {
		return name == null ? "" : name.replaceAll("[^a-zA-Z0-9_]+", "_");
	}
}
