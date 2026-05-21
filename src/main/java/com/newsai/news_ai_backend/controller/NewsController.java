package com.newsai.news_ai_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.newsai.news_ai_backend.dto.NewsFeedDto;
import com.newsai.news_ai_backend.dto.StoryResponseDto;
import com.newsai.news_ai_backend.service.NewsService;

@RestController
@RequestMapping("/api/news")
public class NewsController {

	private final NewsService newsService;

	public NewsController(NewsService newsService) {
		this.newsService = newsService;
	}

	@GetMapping("/feed")
	public List<NewsFeedDto> getFeed(@RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(value = "state", defaultValue = "") String state,
			@RequestParam(value = "city", defaultValue = "") String city,
			@RequestParam(value = "limit", defaultValue = "20") int limit) {
		return newsService.getFeed(category, state, city, limit);
	}

	@GetMapping("/hot")
	public List<NewsFeedDto> getHotNews(@RequestParam(value = "limit", defaultValue = "20") int limit) {
		return newsService.getHotNews(limit);
	}

	@GetMapping("/{id}")
	public StoryResponseDto getStoryById(@PathVariable Long id,
			@RequestParam(value = "language", defaultValue = "ENGLISH") String language,
			@RequestParam(value = "style", defaultValue = "genz") String style,
			@RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
		return newsService.getStoryById(id, language, style, refresh);
	}
}
