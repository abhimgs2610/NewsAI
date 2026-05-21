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

	public String fetchIndiaNewsSince(int hours, int max) {
		if (!hasApiKey()) {
			return "";
		}

		String from = OffsetDateTime.now(ZoneOffset.UTC)
				.minusHours(hours)
				.toString();

		String url = UriComponentsBuilder
				.fromUriString("https://gnews.io/api/v4/search")
				.queryParam("q", "India")
				.queryParam("lang", "en")
				.queryParam("country", "in")
				.queryParam("from", from)
				.queryParam("max", Math.max(1, Math.min(max, 10)))
				.queryParam("apikey", apiKey)
				.build()
				.toUriString();

		return restTemplate.getForObject(url, String.class);
	}
}
