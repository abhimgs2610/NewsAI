package com.newsai.news_ai_backend.client;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GNewsClient {

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${gnews.api-key}")
	private String apiKey;

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

	
	public String fetchDiscoveryNews(String query, String countryCode, int max) {
		if (!hasApiKey()) {
			return "";
		}

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString("https://gnews.io/api/v4/search")
				.queryParam("q", query)
				.queryParam("lang", "en")
				.queryParam("max", Math.max(1, Math.min(max, 10)))
				.queryParam("apikey", apiKey);

		if (countryCode != null && !countryCode.isBlank()) {
			builder.queryParam("country", countryCode);
		}

		return restTemplate.getForObject(builder.build().toUriString(), String.class);
	}
	public String fetchIndiaNewsSince(int hours, int max) {
		return fetchSearchNewsSince("India", "in", hours, max);
	}

	public String fetchWorldNewsSince(int hours, int max) {
		return fetchSearchNewsSince("world", "", hours, max);
	}

	private String fetchSearchNewsSince(String query, String countryCode, int hours, int max) {
		if (!hasApiKey()) {
			return "";
		}

		String from = OffsetDateTime.now(ZoneOffset.UTC)
				.minusHours(hours)
				.toString();

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString("https://gnews.io/api/v4/search")
				.queryParam("q", query)
				.queryParam("lang", "en")
				.queryParam("from", from)
				.queryParam("max", Math.max(1, Math.min(max, 10)))
				.queryParam("apikey", apiKey);

		if (countryCode != null && !countryCode.isBlank()) {
			builder.queryParam("country", countryCode);
		}

		return restTemplate.getForObject(builder.build().toUriString(), String.class);
	}
}
