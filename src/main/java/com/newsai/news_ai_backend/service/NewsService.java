package com.newsai.news_ai_backend.service;

import java.util.List;

import com.newsai.news_ai_backend.dto.NewsFeedDto;
import com.newsai.news_ai_backend.dto.StoryResponseDto;

public interface NewsService {

	List<NewsFeedDto> getFeed(String category, String state, String city, int limit);

	List<NewsFeedDto> getHotNews(int limit);

	StoryResponseDto getStoryById(Long id, String language, String style, boolean refresh);
}
