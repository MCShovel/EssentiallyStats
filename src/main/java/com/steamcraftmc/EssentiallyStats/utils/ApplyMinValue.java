package com.steamcraftmc.EssentiallyStats.utils;

public class ApplyMinValue extends FieldUpdate {

	public ApplyMinValue(String fullName, Long value) {
		super(fullName, value);
	}

	@Override
	public String getAssignment() {
		return String.format("%s = GREATEST(%s, %s)", getField(), getField(), getValue());
	}
}
