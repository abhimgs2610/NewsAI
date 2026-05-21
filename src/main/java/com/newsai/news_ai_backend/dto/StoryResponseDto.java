package com.newsai.news_ai_backend.dto;

public class StoryResponseDto {

	public String story;

	public String getStory() {
		return story;
	}

	public void setStory(String story) {
		this.story = story;
	}

	public StoryResponseDto() {
		super();
	}

	public StoryResponseDto(String story) {
		this.story = story;
	}
}
