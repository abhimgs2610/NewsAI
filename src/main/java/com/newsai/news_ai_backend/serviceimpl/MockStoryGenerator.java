package com.newsai.news_ai_backend.serviceimpl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.newsai.news_ai_backend.service.StoryGenerator;

@Service
@Profile("mock")
public class MockStoryGenerator implements StoryGenerator {

	@Override
	public String buildStory(String title, String context, String category, String style, String language) {
		return "[MOCK STORY][" + language + "][" + style + "] " + title + " (" + category + "): " + context;
	}
}
