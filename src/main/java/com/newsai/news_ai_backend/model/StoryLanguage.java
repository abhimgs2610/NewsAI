package com.newsai.news_ai_backend.model;

public enum StoryLanguage {
	ENGLISH,
	HINDI;

	public static StoryLanguage fromInput(String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			return ENGLISH;
		}
		return switch (rawValue.trim().toUpperCase()) {
		case "ENGLISH" -> ENGLISH;
		case "HINDI" -> HINDI;
		default -> throw new IllegalArgumentException("Unsupported language. Use ENGLISH or HINDI.");
		};
	}
}
