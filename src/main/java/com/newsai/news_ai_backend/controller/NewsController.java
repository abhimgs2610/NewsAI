package com.newsai.news_ai_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.newsai.news_ai_backend.dto.ApiResponseDto;
import com.newsai.news_ai_backend.dto.CountryCountDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryRequestDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryResponseDto;
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
	public ApiResponseDto<List<NewsFeedDto>> getFeed(@RequestParam(value = "country", defaultValue = "") String country,
			@RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(value = "state", defaultValue = "") String state,
			@RequestParam(value = "city", defaultValue = "") String city,
			@RequestParam(value = "q", defaultValue = "") String query,
			@RequestParam(value = "limit", defaultValue = "20") int limit) {
		return ApiResponseDto.success(newsService.getFeed(country, category, state, city, query, limit));
	}

	@PostMapping("/discover")
	public ApiResponseDto<NewsDiscoveryResponseDto> discoverNews(@RequestBody NewsDiscoveryRequestDto request,
			@RequestParam(value = "limit", defaultValue = "10") int limit) {
		NewsDiscoveryResponseDto response = newsService.discoverNews(request, limit);
		return ApiResponseDto.success(response, response.getResults() == null ? 0 : response.getResults().size());
	}

	@GetMapping("/hot")
	public ApiResponseDto<List<NewsFeedDto>> getHotNews(@RequestParam(value = "q", defaultValue = "") String query,
			@RequestParam(value = "limit", defaultValue = "20") int limit) {
		return ApiResponseDto.success(newsService.getHotNews(query, limit));
	}

	@GetMapping("/countries")
	public ApiResponseDto<List<CountryCountDto>> getCountries() {
		return ApiResponseDto.success(newsService.getCountries());
	}

	@GetMapping("/categories")
	public ApiResponseDto<List<String>> getCategories() {
		return ApiResponseDto.success(newsService.getCategories());
	}

	@GetMapping("/states")
	public ApiResponseDto<List<String>> getStates() {
		return ApiResponseDto.success(newsService.getStates());
	}

	@GetMapping("/cities")
	public ApiResponseDto<List<String>> getCities(@RequestParam(value = "state", defaultValue = "") String state) {
		return ApiResponseDto.success(newsService.getCities(state));
	}

	@GetMapping("/{id}")
	public ApiResponseDto<StoryResponseDto> getStoryById(@PathVariable Long id,
			@RequestParam(value = "language", defaultValue = "ENGLISH") String language,
			@RequestParam(value = "style", defaultValue = "genz") String style,
			@RequestParam(value = "refresh", defaultValue = "false") boolean refresh) {
		return ApiResponseDto.success(newsService.getStoryById(id, language, style, refresh), "Record fetched successfully");
	}
}