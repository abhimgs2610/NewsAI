package com.newsai.news_ai_backend.client;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
		requireApiKey("fetching related articles");

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

	
	public String fetchDiscoveryNews(String query, int pageSize) {
		requireApiKey("discovering news");

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/everything")
				.queryParam("q", query)
				.queryParam("language", "en")
				.queryParam("sortBy", "relevancy")
				.queryParam("pageSize", pageSize)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}
	public String fetchIndiaNewsSince(int hours, int pageSize) {
		return fetchSearchNewsSince("india", hours, pageSize);
	}

	public String fetchWorldNewsSince(int hours, int pageSize) {
		return fetchSearchNewsSince("world", hours, pageSize);
	}

	public String fetchIndiaNews(int pageSize) {
		return fetchSearchNews("india", pageSize);
	}

	public String fetchWorldNews(int pageSize) {
		return fetchSearchNews("world", pageSize);
	}

	public String fetchIndiaTopHeadlines(int pageSize) {
		requireApiKey("syncing news");

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/top-headlines")
				.queryParam("country", "in")
				.queryParam("pageSize", pageSize)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}

	private String fetchSearchNewsSince(String query, int hours, int pageSize) {
		requireApiKey("syncing news");

		String from = OffsetDateTime.now(ZoneOffset.UTC)
				.minusHours(hours)
				.toString();

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/everything")
				.queryParam("q", query)
				.queryParam("language", "en")
				.queryParam("from", from)
				.queryParam("sortBy", "publishedAt")
				.queryParam("pageSize", pageSize)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}

	private String fetchSearchNews(String query, int pageSize) {
		requireApiKey("syncing news");

		String url = UriComponentsBuilder
				.fromUriString("https://newsapi.org/v2/everything")
				.queryParam("q", query)
				.queryParam("language", "en")
				.queryParam("sortBy", "publishedAt")
				.queryParam("pageSize", pageSize)
				.queryParam("apiKey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}

	private void requireApiKey(String action) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("NewsAPI key is missing. Set NEWSAPI_API_KEY before " + action + ".");
		}
	}
}
