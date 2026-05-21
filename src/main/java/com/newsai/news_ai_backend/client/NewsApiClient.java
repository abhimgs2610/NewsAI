package com.newsai.news_ai_backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NewsApiClient {

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${newsapi.api-key}")
	private String apiKey;

	public String fetchRelatedArticles(String query) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("NewsAPI key is missing. Set NEWSAPI_API_KEY before fetching related articles.");
		}

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/everything")
				.queryParam("q", query)
				.queryParam("language", "en")
				.queryParam("sortBy", "relevancy")
				.queryParam("pageSize", 5)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}

	public String fetchIndiaNewsSince(int hours, int pageSize) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("NewsAPI key is missing. Set NewsAPI key before syncing news.");
		}

		String from = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
				.minusHours(hours)
				.toString();

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/everything")
				.queryParam("q", "india")
				.queryParam("from", from)
				.queryParam("sortBy", "publishedAt")
				.queryParam("pageSize", pageSize)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}

	public String fetchIndiaNews(int pageSize) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("NewsAPI key is missing. Set NewsAPI key before syncing news.");
		}

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/everything")
				.queryParam("q", "india")
				.queryParam("sortBy", "publishedAt")
				.queryParam("pageSize", pageSize)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}

	public String fetchIndiaTopHeadlines(int pageSize) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("NewsAPI key is missing. Set NewsAPI key before syncing news.");
		}

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/top-headlines")
				.queryParam("country", "in")
				.queryParam("pageSize", pageSize)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}
	
}
