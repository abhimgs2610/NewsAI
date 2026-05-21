package com.newsai.news_ai_backend.serviceimpl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsai.news_ai_backend.client.NewsApiClient;
import com.newsai.news_ai_backend.model.NewsArticle;

@Service
public class ArticleResearchService {

	private static final Logger logger = LoggerFactory.getLogger(ArticleResearchService.class);
	private static final int MAX_BACKGROUND_CHARS = 2500;
	private static final Set<String> STOP_WORDS = Set.of("the", "a", "an", "and", "or", "but", "for", "from",
			"with", "without", "into", "onto", "over", "under", "this", "that", "these", "those", "about", "after",
			"before", "says", "said", "will", "would", "could", "should", "what", "when", "where", "why", "how",
			"news", "latest", "update", "updates");

	private final NewsApiClient newsApiClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ArticleResearchService(NewsApiClient newsApiClient) {
		this.newsApiClient = newsApiClient;
	}

	public String research(NewsArticle article) {
		String query = buildQuery(article.getTitle(), "");
		if (query.isBlank()) {
			return "";
		}

		try {
			String response = newsApiClient.fetchRelatedArticles(query);
			JsonNode articles = objectMapper.readTree(response).path("articles");
			if (!articles.isArray()) {
				return "";
			}

			StringBuilder context = new StringBuilder();
			int count = 0;

			for (JsonNode item : articles) {
				if (count >= 4) {
					break;
				}

				String url = textValue(item, "url");
				String title = textValue(item, "title");
				if (sameUrl(article.getUrl(), url) || title.isBlank()) {
					continue;
				}

				String source = item.path("source").path("name").asText("");
				String publishedAt = textValue(item, "publishedAt");
				String description = firstNonBlank(textValue(item, "description"), textValue(item, "content"));

				context.append("- ");
				if (!source.isBlank()) {
					context.append(source).append(": ");
				}
				context.append(title);
				if (!publishedAt.isBlank()) {
					context.append(" (").append(publishedAt).append(")");
				}
				if (!description.isBlank()) {
					context.append(" - ").append(clean(description));
				}
				context.append("\n");
				count++;
			}

			String background = trim(context.toString(), MAX_BACKGROUND_CHARS);
			logger.info("Background research query: {}", query);
			logger.info("Background research context: {}", background);
			return background;
		} catch (Exception e) {
			logger.warn("Could not fetch background research for query: {}", query, e);
			return "";
		}
	}

	private String buildQuery(String title, String category) {
		String words = Arrays.stream(clean(title).toLowerCase(Locale.ROOT).split("\\s+"))
				.map(word -> word.replaceAll("[^a-z0-9]", ""))
				.filter(word -> word.length() > 3)
				.filter(word -> !STOP_WORDS.contains(word))
				.distinct()
				.limit(8)
				.collect(Collectors.joining(" "));

		Set<String> queryParts = new LinkedHashSet<>();
		if (category != null && !category.isBlank()) {
			queryParts.add(category.trim());
		}
		if (!words.isBlank()) {
			queryParts.add(words);
		}

		return trim(String.join(" ", queryParts), 250);
	}

	private String textValue(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		return value.isMissingNode() || value.isNull() ? "" : value.asText("");
	}

	private String firstNonBlank(String first, String fallback) {
		return first == null || first.isBlank() ? fallback : first;
	}

	private boolean sameUrl(String first, String second) {
		return first != null && second != null && first.equalsIgnoreCase(second);
	}

	private String clean(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("\\[\\+\\d+ chars\\]", "")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private String trim(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value == null ? "" : value;
		}
		return value.substring(0, maxLength).trim();
	}
}
