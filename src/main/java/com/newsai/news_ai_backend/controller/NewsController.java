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
import com.newsai.news_ai_backend.dto.NewsAskRequestDto;
import com.newsai.news_ai_backend.dto.NewsAskResponseDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryRequestDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryResponseDto;
import com.newsai.news_ai_backend.dto.NewsFeedDto;
import com.newsai.news_ai_backend.dto.StoryResponseDto;
import com.newsai.news_ai_backend.service.NewsService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/news")
public class NewsController {

	private static final Set<String> FEED_QUERY_PARAMS = Set.of("country", "category", "state", "city", "q", "limit",
			"offset", "searchValue");
	private static final Set<String> HOT_QUERY_PARAMS = Set.of("q", "limit", "offset");
	private static final Set<String> DISCOVER_QUERY_PARAMS = Set.of();
	private static final int DEFAULT_FEED_LIMIT = 20;
	private static final int MAX_FEED_LIMIT = 100;

	private final NewsService newsService;

	public NewsController(NewsService newsService) {
		this.newsService = newsService;
	}

	@GetMapping("/feed")
	public ApiResponseDto<List<NewsFeedDto>> getFeed(@RequestParam(value = "country", defaultValue = "India") String country,
			@RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(value = "state", defaultValue = "") String state,
			@RequestParam(value = "city", defaultValue = "") String city,
			@RequestParam(value = "q", defaultValue = "") String query,
			@RequestParam(value = "limit", required = false) String limit,
			@RequestParam(value = "offset", required = false) String offset,
			@RequestParam(value = "searchValue", required = false) String searchValue,
			HttpServletRequest request) {
		validateQueryParams(request, FEED_QUERY_PARAMS);
		int parsedLimit = parseLimit(rawQueryValue(request, "limit"), limit, DEFAULT_FEED_LIMIT, MAX_FEED_LIMIT);
		int parsedOffset = parseOffset(rawQueryValue(request, "offset"), offset);
		boolean globalSearch = parseBoolean(rawQueryValue(request, "searchValue"), searchValue, false, "searchValue");
		if (parsedLimit == 0) {
			return ApiResponseDto.success(Collections.emptyList());
		}
		String effectiveCountry = globalSearch ? "" : country;
		String effectiveCategory = globalSearch ? "" : category;
		String effectiveState = globalSearch ? "" : state;
		String effectiveCity = globalSearch ? "" : city;
		return ApiResponseDto
				.success(newsService.getFeed(effectiveCountry, effectiveCategory, effectiveState, effectiveCity, query,
						parsedLimit, parsedOffset));
	}

	@PostMapping("/discover")
	public ApiResponseDto<NewsDiscoveryResponseDto> discoverNews(@RequestBody NewsDiscoveryRequestDto requestBody,
			HttpServletRequest request) {
		validateQueryParams(request, DISCOVER_QUERY_PARAMS);
		NewsDiscoveryResponseDto response = newsService.discoverNews(requestBody);
		return ApiResponseDto.success(response, response.getResults() == null ? 0 : response.getResults().size(),
				response.getMessage());
	}

	@GetMapping("/hot")
	public ApiResponseDto<List<NewsFeedDto>> getHotNews(@RequestParam(value = "q", defaultValue = "") String query,
			@RequestParam(value = "limit", required = false) String limit,
			@RequestParam(value = "offset", required = false) String offset,
			HttpServletRequest request) {
		validateQueryParams(request, HOT_QUERY_PARAMS);
		int parsedLimit = parseLimit(rawQueryValue(request, "limit"), limit, DEFAULT_FEED_LIMIT, MAX_FEED_LIMIT);
		int parsedOffset = parseOffset(rawQueryValue(request, "offset"), offset);
		if (parsedLimit == 0) {
			return ApiResponseDto.success(Collections.emptyList());
		}
		return ApiResponseDto.success(newsService.getHotNews(query, parsedLimit, parsedOffset));
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

	@PostMapping("/{id}/ask")
	public ApiResponseDto<NewsAskResponseDto> askNewsQuestion(@PathVariable Long id,
			@RequestBody NewsAskRequestDto requestBody) {
		return ApiResponseDto.success(newsService.askNewsQuestion(id, requestBody), "Record fetched successfully");
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

	private int parseOffset(String rawOffset, String decodedOffset) {
		if (rawOffset == null && decodedOffset == null) {
			return 0;
		}
		String valueToValidate = rawOffset == null ? decodedOffset : rawOffset;
		if (valueToValidate == null || valueToValidate.isBlank() || !valueToValidate.matches("[0-9]+")) {
			throw new IllegalArgumentException("offset must be an integer.");
		}
		try {
			return Integer.parseInt(valueToValidate);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("offset must be an integer.", e);
		}
	}

	private boolean parseBoolean(String rawValue, String decodedValue, boolean defaultValue, String paramName) {
		if (rawValue == null && decodedValue == null) {
			return defaultValue;
		}
		String valueToValidate = rawValue == null ? decodedValue : rawValue;
		if (valueToValidate == null || valueToValidate.isBlank()) {
			throw new IllegalArgumentException(paramName + " must be true or false.");
		}
		if ("true".equalsIgnoreCase(valueToValidate)) {
			return true;
		}
		if ("false".equalsIgnoreCase(valueToValidate)) {
			return false;
		}
		throw new IllegalArgumentException(paramName + " must be true or false.");
	}
}