package com.steamcraftmc.EssentiallyStats.utils;

public class NameValuePair<TValue> {
	public final String Name;
	public final TValue Value;
	
	public NameValuePair(String name, TValue value) {
		this.Name = name;
		this.Value = value;
	}
}
