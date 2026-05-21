package com.newsai.news_ai_backend.dto;

public class LocationBackfillResponseDto {

	private int scanned;
	private int updated;
	private int unchanged;

	public LocationBackfillResponseDto() {
	}

	public LocationBackfillResponseDto(int scanned, int updated, int unchanged) {
		this.scanned = scanned;
		this.updated = updated;
		this.unchanged = unchanged;
	}

	public int getScanned() {
		return scanned;
	}

	public void setScanned(int scanned) {
		this.scanned = scanned;
	}

	public int getUpdated() {
		return updated;
	}

	public void setUpdated(int updated) {
		this.updated = updated;
	}

	public int getUnchanged() {
		return unchanged;
	}

	public void setUnchanged(int unchanged) {
		this.unchanged = unchanged;
	}
}
