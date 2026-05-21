package com.newsai.news_ai_backend.client;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenAiClient {

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${openai.api-key}")
	private String apiKey;

	@Value("${openai.model}")
	private String model;

	@Value("${openai.url}")
	private String url;

	public String generateStory(String prompt) {
		return generateCompletion(prompt, 0.25, 1200);
	}

	public String generateEnrichment(String prompt) {
		return generateCompletion(prompt, 0.1, 320);
	}

	private String generateCompletion(String prompt, double temperature, int maxTokens) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("OpenAI API key is missing. Set OPENAI_API_KEY before generating stories.");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(apiKey);

		Map<String, Object> body = new HashMap<>();
		body.put("model", model);
		body.put("temperature", temperature);
		body.put("max_tokens", maxTokens);

		List<Map<String, String>> messages = new ArrayList<>();

		Map<String, String> message = new HashMap<>();
		message.put("role", "user");
		message.put("content", prompt);

		messages.add(message);

		body.put("messages", messages);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(chatCompletionsUrl(), request, String.class);

		return extractContent(response.getBody());
	}

	private String chatCompletionsUrl() {
		String configuredUrl = url == null ? "" : url.trim();
		if (configuredUrl.endsWith("/chat/completions")) {
			return configuredUrl;
		}
		return configuredUrl.replaceAll("/+$", "") + "/chat/completions";
	}

	private String extractContent(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			JsonNode content = root.path("choices").path(0).path("message").path("content");
			if (content.isMissingNode() || content.asText().isBlank()) {
				throw new IllegalStateException("OpenAI response did not contain story content.");
			}
			return content.asText().trim();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to parse OpenAI response.", e);
		}
	}
}
