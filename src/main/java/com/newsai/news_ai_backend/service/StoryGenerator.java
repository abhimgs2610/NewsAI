package com.newsai.news_ai_backend.service;

public interface StoryGenerator {

	String buildStory(String title, String context, String category, String style, String language);
}
