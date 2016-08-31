package com.steamcraftmc.EssentiallyStats.utils;

public class UpdateStatValue extends FieldUpdate {

	private Long delta;

	public UpdateStatValue(String fullName, Long value, Long delta) {
		super(fullName, value);
		this.delta = delta;
	}

	@Override
	public String getAssignment() {
		return String.format("%s = (%s + %s)", getField(), getField(), String.valueOf(this.delta));
	}
}
