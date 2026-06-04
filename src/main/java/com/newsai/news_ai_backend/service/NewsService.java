package com.newsai.news_ai_backend.service;

import java.util.List;

import com.newsai.news_ai_backend.dto.CountryCountDto;
import com.newsai.news_ai_backend.dto.NewsAskRequestDto;
import com.newsai.news_ai_backend.dto.NewsAskResponseDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryRequestDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryResponseDto;
import com.newsai.news_ai_backend.dto.NewsFeedDto;
import com.newsai.news_ai_backend.dto.StoryResponseDto;

public interface NewsService {

	List<NewsFeedDto> getFeed(String country, String category, String state, String city, String query, int limit);

	NewsDiscoveryResponseDto discoverNews(NewsDiscoveryRequestDto request, int limit);

	List<NewsFeedDto> getHotNews(String query, int limit);

	List<CountryCountDto> getCountries();

	List<String> getCategories();

	List<String> getStates();

	List<String> getCities(String state);

	StoryResponseDto getStoryById(Long id, String language, String style, boolean refresh);

	NewsAskResponseDto askNewsQuestion(Long id, NewsAskRequestDto request);
}


