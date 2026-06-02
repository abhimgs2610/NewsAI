package com.newsai.news_ai_backend.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/news")
public class NewsController {

	private static final Set<String> FEED_QUERY_PARAMS = Set.of("country", "category", "state", "city", "q", "limit");
	private static final Set<String> HOT_QUERY_PARAMS = Set.of("q", "limit");
	private static final Set<String> DISCOVER_QUERY_PARAMS = Set.of("limit");
	private static final int DEFAULT_FEED_LIMIT = 20;
	private static final int DEFAULT_DISCOVER_LIMIT = 10;
	private static final int MAX_FEED_LIMIT = 100;
	private static final int MAX_DISCOVER_LIMIT = 20;

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
			@RequestParam(value = "limit", required = false) String limit,
			HttpServletRequest request) {
		validateQueryParams(request, FEED_QUERY_PARAMS);
		int parsedLimit = parseLimit(rawQueryValue(request, "limit"), limit, DEFAULT_FEED_LIMIT, MAX_FEED_LIMIT);
		if (parsedLimit == 0) {
			return ApiResponseDto.success(Collections.emptyList());
		}
		return ApiResponseDto.success(newsService.getFeed(country, category, state, city, query, parsedLimit));
	}

	@PostMapping("/discover")
	public ApiResponseDto<NewsDiscoveryResponseDto> discoverNews(@RequestBody NewsDiscoveryRequestDto requestBody,
			@RequestParam(value = "limit", required = false) String limit,
			HttpServletRequest request) {
		validateQueryParams(request, DISCOVER_QUERY_PARAMS);
		int parsedLimit = parseLimit(rawQueryValue(request, "limit"), limit, DEFAULT_DISCOVER_LIMIT, MAX_DISCOVER_LIMIT);
		if (parsedLimit == 0) {
			NewsDiscoveryResponseDto emptyResponse = new NewsDiscoveryResponseDto("NO_MATCH_FOUND",
					"No records requested.", "", Collections.emptyList());
			return ApiResponseDto.success(emptyResponse, 0);
		}
		NewsDiscoveryResponseDto response = newsService.discoverNews(requestBody, parsedLimit);
		return ApiResponseDto.success(response, response.getResults() == null ? 0 : response.getResults().size());
	}

	@GetMapping("/hot")
	public ApiResponseDto<List<NewsFeedDto>> getHotNews(@RequestParam(value = "q", defaultValue = "") String query,
			@RequestParam(value = "limit", required = false) String limit,
			HttpServletRequest request) {
		validateQueryParams(request, HOT_QUERY_PARAMS);
		int parsedLimit = parseLimit(rawQueryValue(request, "limit"), limit, DEFAULT_FEED_LIMIT, MAX_FEED_LIMIT);
		if (parsedLimit == 0) {
			return ApiResponseDto.success(Collections.emptyList());
		}
		return ApiResponseDto.success(newsService.getHotNews(query, parsedLimit));
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

	private void validateQueryParams(HttpServletRequest request, Set<String> allowedParams) {
		String rawQuery = request.getQueryString();
		if (rawQuery != null && rawQuery.endsWith("&")) {
			throw new IllegalArgumentException("Invalid query parameter.");
		}

		if (rawQuery != null) {
			for (String part : rawQuery.split("&", -1)) {
				if (part.isBlank()) {
					throw new IllegalArgumentException("Invalid query parameter.");
				}
				String rawName = part.split("=", 2)[0];
				if (!allowedParams.contains(rawName)) {
					throw new IllegalArgumentException("Invalid query parameter.");
				}
			}
		}

		for (String paramName : request.getParameterMap().keySet()) {
			if (!allowedParams.contains(paramName)) {
				throw new IllegalArgumentException("Invalid query parameter.");
			}
		}
	}

	private String rawQueryValue(HttpServletRequest request, String name) {
		String rawQuery = request.getQueryString();
		if (rawQuery == null || rawQuery.isBlank()) {
			return null;
		}
		for (String part : rawQuery.split("&", -1)) {
			String[] nameAndValue = part.split("=", 2);
			if (nameAndValue.length == 2 && name.equals(nameAndValue[0])) {
				return nameAndValue[1];
			}
		}
		return null;
	}

	private int parseLimit(String rawLimit, String decodedLimit, int defaultLimit, int maxLimit) {
		if (rawLimit == null && decodedLimit == null) {
			return defaultLimit;
		}
		String valueToValidate = rawLimit == null ? decodedLimit : rawLimit;
		if (valueToValidate == null || valueToValidate.isBlank() || !valueToValidate.matches("[0-9]+")) {
			throw new IllegalArgumentException("limit must be an integer.");
		}
		int parsedLimit;
		try {
			parsedLimit = Integer.parseInt(valueToValidate);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("limit must be an integer.", e);
		}
		if (parsedLimit > maxLimit) {
			throw new IllegalArgumentException("limit exceeds maximum allowed value.");
		}
		return parsedLimit;
	}
}