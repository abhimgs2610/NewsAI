package com.newsai.news_ai_backend.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsai.news_ai_backend.client.GNewsClient;
import com.newsai.news_ai_backend.client.NewsApiClient;
import com.newsai.news_ai_backend.client.OpenAiClient;
import com.newsai.news_ai_backend.dto.NewsSyncResponseDto;
import com.newsai.news_ai_backend.model.NewsAiEnrichment;
import com.newsai.news_ai_backend.model.NewsArticle;
import com.newsai.news_ai_backend.repository.NewsAiEnrichmentRepository;
import com.newsai.news_ai_backend.repository.NewsArticleRepository;

@Service
public class NewsSyncService {

	private static final Logger logger = LoggerFactory.getLogger(NewsSyncService.class);
	private static final int DEFAULT_HOURS = 24;
	private static final int NEWSAPI_PAGE_SIZE = 100;
	private static final int GNEWS_PAGE_SIZE = 10;
	private static final int ENRICHMENT_LIMIT = 5;
	private static final int ENRICHMENT_RETRY_LIMIT = 3;
	private static final int TITLE_LIMIT = 1000;
	private static final int DESCRIPTION_LIMIT = 1000;
	private static final int CONTENT_LIMIT = 2000;
	private static final int URL_LIMIT = 1000;
	private static final int SOURCE_LIMIT = 255;

	private final NewsApiClient newsApiClient;
	private final GNewsClient gNewsClient;
	private final OpenAiClient openAiClient;
	private final NewsArticleRepository articleRepository;
	private final NewsAiEnrichmentRepository enrichmentRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public NewsSyncService(NewsApiClient newsApiClient, GNewsClient gNewsClient, OpenAiClient openAiClient,
			NewsArticleRepository articleRepository, NewsAiEnrichmentRepository enrichmentRepository) {
		this.newsApiClient = newsApiClient;
		this.gNewsClient = gNewsClient;
		this.openAiClient = openAiClient;
		this.articleRepository = articleRepository;
		this.enrichmentRepository = enrichmentRepository;
	}

	public NewsSyncResponseDto syncLatestIndiaNews() {
		return syncLatestIndiaNews(DEFAULT_HOURS);
	}

	public NewsSyncResponseDto syncLatestIndiaNews(int hours) {
		try {
			Set<String> seenUrls = new LinkedHashSet<>();
			Set<Long> candidateArticleIds = new LinkedHashSet<>();
			SyncCounter counter = new SyncCounter();

			syncNewsApi(hours, seenUrls, candidateArticleIds, counter);
			syncGNews(hours, seenUrls, candidateArticleIds, counter);
			enrichArticles(candidateArticleIds, counter);

			logger.info("News sync completed. fetched={}, saved={}, updated={}, skipped={}, enriched={}",
					counter.fetched, counter.saved, counter.updated, counter.skipped, counter.enriched);
			return toResponse(counter);
		} catch (Exception e) {
			throw new RuntimeException("Could not sync latest India news", e);
		}
	}

	private void syncNewsApi(int hours, Set<String> seenUrls, Set<Long> candidateArticleIds, SyncCounter counter)
			throws Exception {
		String response = newsApiClient.fetchIndiaNewsSince(hours, NEWSAPI_PAGE_SIZE);
		JsonNode articles = objectMapper.readTree(response).path("articles");
		if (!articles.isArray()) {
			throw new IllegalStateException("NewsAPI response did not contain an articles list.");
		}

		if (articles.isEmpty()) {
			logger.info("NewsAPI last-{}h search returned 0 articles. Trying plain India search fallback.", hours);
			response = newsApiClient.fetchIndiaNews(NEWSAPI_PAGE_SIZE);
			articles = objectMapper.readTree(response).path("articles");
		}

		if (!articles.isArray()) {
			throw new IllegalStateException("NewsAPI plain India response did not contain an articles list.");
		}

		if (articles.isEmpty()) {
			logger.info("NewsAPI plain India search returned 0 articles. Trying India top-headlines fallback.");
			response = newsApiClient.fetchIndiaTopHeadlines(NEWSAPI_PAGE_SIZE);
			articles = objectMapper.readTree(response).path("articles");
		}

		if (!articles.isArray()) {
			throw new IllegalStateException("NewsAPI top-headlines response did not contain an articles list.");
		}

		counter.fetched += articles.size();
		counter.newsApiFetched += articles.size();
		for (JsonNode article : articles) {
			saveRawArticle(article, "NewsAPI", "urlToImage", seenUrls, candidateArticleIds, counter);
		}
	}

	private void syncGNews(int hours, Set<String> seenUrls, Set<Long> candidateArticleIds, SyncCounter counter)
			throws Exception {
		if (!gNewsClient.hasApiKey()) {
			logger.info("GNews sync skipped because gnews.api-key is not configured.");
			return;
		}

		String response = gNewsClient.fetchIndiaNewsSince(hours, GNEWS_PAGE_SIZE);
		if (response == null || response.isBlank()) {
			return;
		}

		JsonNode articles = objectMapper.readTree(response).path("articles");
		if (!articles.isArray()) {
			logger.warn("GNews response did not contain an articles list.");
			return;
		}

		counter.fetched += articles.size();
		counter.gNewsFetched += articles.size();
		for (JsonNode article : articles) {
			saveRawArticle(article, "GNews", "image", seenUrls, candidateArticleIds, counter);
		}
	}

	private void saveRawArticle(JsonNode node, String provider, String imageField, Set<String> seenUrls,
			Set<Long> candidateArticleIds, SyncCounter counter) {
		String title = trimToLength(textValue(node, "title"), TITLE_LIMIT);
		String description = trimToLength(textValue(node, "description"), DESCRIPTION_LIMIT);
		String content = cleanContent(firstNonBlank(textValue(node, "content"), description));
		String rawUrl = textValue(node, "url");
		String url = trimToLength(rawUrl, URL_LIMIT);
		String source = trimToLength(node.path("source").path("name").asText(""), SOURCE_LIMIT);

		if (title.isBlank() || url.isBlank() || !seenUrls.add(url)) {
			counter.skipped++;
			incrementProviderSkipped(provider, counter);
			return;
		}

		NewsArticle article = articleRepository.findByUrl(url).orElseGet(NewsArticle::new);
		boolean isNew = article.getId() == null;

		article.setUrl(url);
		article.setTitle(title);
		article.setDescription(description);
		article.setContent(content);
		article.setSource(source);
		article.setProvider(provider);
		article.setImageUrl(trimToLength(textValue(node, imageField), URL_LIMIT));
		article.setPublishedAt(parsePublishedDate(textValue(node, "publishedAt")));

		article = articleRepository.save(article);
		candidateArticleIds.add(article.getId());

		if (isNew) {
			counter.saved++;
			incrementProviderSaved(provider, counter);
		} else {
			counter.updated++;
			incrementProviderUpdated(provider, counter);
		}
	}

	private void enrichArticles(Set<Long> candidateArticleIds, SyncCounter counter) {
		if (candidateArticleIds.isEmpty()) {
			return;
		}

		List<Long> selectedIds = candidateArticleIds.stream().limit(ENRICHMENT_LIMIT).toList();
		List<NewsArticle> articles = articleRepository.findByIdIn(selectedIds);
		List<NewsArticle> ordered = new ArrayList<>();
		for (Long id : selectedIds) {
			articles.stream().filter(a -> a.getId().equals(id)).findFirst().ifPresent(ordered::add);
		}

		for (NewsArticle article : ordered) {
			try {
				EnrichmentResult result = enrichArticleWithRetry(article);
				saveEnrichment(article, result);
				counter.enriched++;
			} catch (Exception e) {
				logger.warn("Enrichment failed for article id={}", article.getId(), e);
				counter.enrichmentSkipped++;
			}
		}

		int nonProcessed = Math.max(0, candidateArticleIds.size() - ENRICHMENT_LIMIT);
		counter.enrichmentSkipped += nonProcessed;
	}

	private EnrichmentResult enrichArticleWithRetry(NewsArticle article) throws Exception {
		int attempt = 0;
		while (attempt < ENRICHMENT_RETRY_LIMIT) {
			try {
				return enrichArticleWithGroq(article);
			} catch (HttpClientErrorException.TooManyRequests e) {
				attempt++;
				long waitMs = parseRetryMillis(e.getResponseBodyAsString(), attempt);
				logger.warn("Groq rate limited for article id={}, attempt={}/{}. Waiting {} ms.",
						article.getId(), attempt, ENRICHMENT_RETRY_LIMIT, waitMs);
				Thread.sleep(waitMs);
			}
		}
		throw new IllegalStateException(
				"Enrichment retries exhausted due to rate limit for article id=" + article.getId());
	}

	private EnrichmentResult enrichArticleWithGroq(NewsArticle article) throws Exception {
		String prompt = """
				You are a strict JSON generator for news enrichment.
				Read the provided raw news inputs and return ONLY valid JSON with this exact schema:
				{
				  "category": "string",
				  "goodHeadline": "string",
				  "briefStory": "string",
				  "importanceScore": 0,
				  "country": "string",
				  "state": "string",
				  "city": "string"
				}

				Rules:
				- Decide category naturally. Do not force predefined categories.
				- goodHeadline must be concise, clean and human-friendly.
				- briefStory must be 1-3 sentences.
				- importanceScore must be an integer from 0 to 100.
				- country/state/city can be empty strings if unknown.
				- Do not include markdown, explanations, or extra keys.

				Raw title: %s
				Raw description: %s
				Raw content: %s
				Raw source: %s
				Raw url: %s
				Raw published date: %s
				""".formatted(safeForPrompt(article.getTitle(), 240), safeForPrompt(article.getDescription(), 700),
						safeForPrompt(article.getContent(), 700), safe(article.getSource()), safe(article.getUrl()),
						article.getPublishedAt() == null ? "" : article.getPublishedAt().toString());

		String response = openAiClient.generateEnrichment(prompt);
		String jsonPayload = extractJson(response);
		JsonNode root = objectMapper.readTree(jsonPayload);

		EnrichmentResult result = new EnrichmentResult();
		result.category = trimToLength(textValue(root, "category"), 255);
		result.goodHeadline = trimToLength(textValue(root, "goodHeadline"), TITLE_LIMIT);
		result.briefStory = trimToLength(textValue(root, "briefStory"), CONTENT_LIMIT);
		result.country = trimToLength(textValue(root, "country"), 255);
		result.state = trimToLength(textValue(root, "state"), 255);
		result.city = trimToLength(textValue(root, "city"), 255);

		int score = root.path("importanceScore").asInt(50);
		if (score < 0) {
			score = 0;
		}
		if (score > 100) {
			score = 100;
		}
		result.importanceScore = score;

		if (result.category.isBlank()) {
			result.category = "general";
		}
		if (result.goodHeadline.isBlank()) {
			result.goodHeadline = trimToLength(firstNonBlank(article.getTitle(), ""), TITLE_LIMIT);
		}
		if (result.briefStory.isBlank()) {
			result.briefStory = trimToLength(firstNonBlank(article.getDescription(), article.getContent()),
					CONTENT_LIMIT);
		}
		if (result.country.isBlank()) {
			result.country = "india";
		}
		return result;
	}

	private void saveEnrichment(NewsArticle article, EnrichmentResult result) {
		NewsAiEnrichment enrichment = enrichmentRepository.findByNewsArticleId(article.getId())
				.orElseGet(NewsAiEnrichment::new);
		enrichment.setNewsArticle(article);
		enrichment.setCategory(result.category);
		enrichment.setGoodHeadline(result.goodHeadline);
		enrichment.setBriefStory(result.briefStory);
		enrichment.setImportanceScore(result.importanceScore);
		enrichment.setCountry(result.country);
		enrichment.setState(result.state);
		enrichment.setCity(result.city);
		enrichment.setProcessedAt(LocalDateTime.now());
		enrichmentRepository.save(enrichment);
	}

	private NewsSyncResponseDto toResponse(SyncCounter counter) {
		NewsSyncResponseDto response = new NewsSyncResponseDto(counter.fetched, counter.saved, counter.updated,
				counter.skipped);
		response.setNewsApiFetched(counter.newsApiFetched);
		response.setNewsApiSaved(counter.newsApiSaved);
		response.setNewsApiUpdated(counter.newsApiUpdated);
		response.setNewsApiSkipped(counter.newsApiSkipped);
		response.setGNewsFetched(counter.gNewsFetched);
		response.setGNewsSaved(counter.gNewsSaved);
		response.setGNewsUpdated(counter.gNewsUpdated);
		response.setGNewsSkipped(counter.gNewsSkipped);
		response.setEnriched(counter.enriched);
		response.setEnrichmentSkipped(counter.enrichmentSkipped);
		return response;
	}

	private void incrementProviderSaved(String provider, SyncCounter counter) {
		if ("NewsAPI".equals(provider)) {
			counter.newsApiSaved++;
		} else if ("GNews".equals(provider)) {
			counter.gNewsSaved++;
		}
	}

	private void incrementProviderUpdated(String provider, SyncCounter counter) {
		if ("NewsAPI".equals(provider)) {
			counter.newsApiUpdated++;
		} else if ("GNews".equals(provider)) {
			counter.gNewsUpdated++;
		}
	}

	private void incrementProviderSkipped(String provider, SyncCounter counter) {
		if ("NewsAPI".equals(provider)) {
			counter.newsApiSkipped++;
		} else if ("GNews".equals(provider)) {
			counter.gNewsSkipped++;
		}
	}

	private String textValue(JsonNode node, String fieldName) {
		JsonNode value = node.path(fieldName);
		return value.isMissingNode() || value.isNull() ? "" : value.asText("");
	}

	private String firstNonBlank(String first, String fallback) {
		return first == null || first.isBlank() ? fallback : first;
	}

	private String cleanContent(String content) {
		String cleaned = content == null ? "" : content;
		cleaned = cleaned.replaceAll("\\[\\+\\d+ chars\\]", "");
		cleaned = cleaned.replaceAll("\\.{3,}", "");
		cleaned = cleaned.replace("\u2026", "");
		return trimToLength(cleaned, CONTENT_LIMIT);
	}

	private String trimToLength(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.length() <= maxLength) {
			return trimmed;
		}
		return trimmed.substring(0, maxLength).trim();
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private String safeForPrompt(String value, int maxLength) {
		String cleaned = safe(value);
		if (cleaned.length() <= maxLength) {
			return cleaned;
		}
		return cleaned.substring(0, maxLength).trim();
	}

	private long parseRetryMillis(String responseBody, int attempt) {
		if (responseBody != null) {
			Matcher matcher = Pattern.compile("Please try again in\\s+([0-9]+(?:\\.[0-9]+)?)s").matcher(responseBody);
			if (matcher.find()) {
				double seconds = Double.parseDouble(matcher.group(1));
				return (long) ((seconds + 0.5) * 1000);
			}
		}
		return 1500L * attempt;
	}

	private LocalDate parsePublishedDate(String publishedAt) {
		if (publishedAt == null || publishedAt.isBlank()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(publishedAt).toLocalDate();
		} catch (Exception e) {
			return null;
		}
	}

	private String extractJson(String response) {
		String value = response == null ? "" : response.trim();
		if (value.startsWith("```")) {
			value = value.replaceFirst("(?s)^```(?:json)?\\s*", "");
			value = value.replaceFirst("(?s)\\s*```$", "");
		}

		int firstBrace = value.indexOf('{');
		int lastBrace = value.lastIndexOf('}');
		if (firstBrace >= 0 && lastBrace > firstBrace) {
			return value.substring(firstBrace, lastBrace + 1);
		}
		return value;
	}

	private static class SyncCounter {
		private int fetched;
		private int saved;
		private int updated;
		private int skipped;
		private int newsApiFetched;
		private int newsApiSaved;
		private int newsApiUpdated;
		private int newsApiSkipped;
		private int gNewsFetched;
		private int gNewsSaved;
		private int gNewsUpdated;
		private int gNewsSkipped;
		private int enriched;
		private int enrichmentSkipped;
	}

	private static class EnrichmentResult {
		private String category;
		private String goodHeadline;
		private String briefStory;
		private Integer importanceScore;
		private String country;
		private String state;
		private String city;
	}
}
