package com.newsai.news_ai_backend.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
	private final BatchPauseService batchPauseService;
	private final ObjectMapper objectMapper = new ObjectMapper();


	@Value("${enrichment.retry.count:3}")
	private int enrichmentRetryCount;

	@Value("${enrichment.retry.baseDelayMs:1500}")
	private long enrichmentRetryBaseDelayMs;

	public NewsSyncService(NewsApiClient newsApiClient, GNewsClient gNewsClient, OpenAiClient openAiClient,
			NewsArticleRepository articleRepository, NewsAiEnrichmentRepository enrichmentRepository, BatchPauseService batchPauseService) {
		this.newsApiClient = newsApiClient;
		this.gNewsClient = gNewsClient;
		this.openAiClient = openAiClient;
		this.articleRepository = articleRepository;
		this.enrichmentRepository = enrichmentRepository;
		this.batchPauseService = batchPauseService;
	}

	public NewsSyncResponseDto syncLatestIndiaNews() {
		return syncLatestIndiaNews(DEFAULT_HOURS);
	}

	public NewsSyncResponseDto syncLatestIndiaNews(int hours) {
		return syncLatestNews(hours);
	}

	public NewsSyncResponseDto syncLatestNews(int hours) {
		try {
			Set<String> seenUrls = new LinkedHashSet<>();
			Set<Long> candidateArticleIds = new LinkedHashSet<>();
			SyncCounter counter = new SyncCounter();

			ensureBatchNotPaused();
			syncNewsApiIndia(hours, seenUrls, candidateArticleIds, counter);
			ensureBatchNotPaused();
			syncNewsApiWorld(hours, seenUrls, candidateArticleIds, counter);
			ensureBatchNotPaused();
			syncGNewsIndia(hours, seenUrls, candidateArticleIds, counter);
			ensureBatchNotPaused();
			syncGNewsWorld(hours, seenUrls, candidateArticleIds, counter);
			ensureBatchNotPaused();
			enrichArticles(candidateArticleIds, counter, true);

			logger.info("News sync completed. fetched={}, saved={}, updated={}, skipped={}, enriched={}",
					counter.fetched, counter.saved, counter.updated, counter.skipped, counter.enriched);
			return toResponse(counter);
		} catch (BatchPausedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Could not sync latest India and World news", e);
		}
	}

	
	public Optional<NewsAiEnrichment> enrichArticleIfNeeded(NewsArticle article) {
		Optional<NewsAiEnrichment> existing = enrichmentRepository.findByNewsArticleId(article.getId());
		if (existing.isPresent()) {
			return existing;
		}
		try {
			EnrichmentResult result = enrichArticleWithRetry(article);
			saveEnrichment(article, result);
			return enrichmentRepository.findByNewsArticleId(article.getId());
		} catch (Exception e) {
			logger.warn("Discovery enrichment failed for article id={}", article.getId(), e);
			return Optional.empty();
		}
	}

	public Optional<NewsAiEnrichment> enrichArticleForDiscovery(NewsArticle article, String country, String state,
			String city) {
		return enrichArticleIfNeeded(article)
				.map(enrichment -> applyDiscoveryLocationOverrides(enrichment, country, state, city));
	}

	public NewsAiEnrichment applyDiscoveryLocationOverrides(NewsAiEnrichment enrichment, String country, String state,
			String city) {
		String cleanedCountry = trimToLength(country, SOURCE_LIMIT);
		String cleanedState = trimToLength(state, SOURCE_LIMIT);
		String cleanedCity = trimToLength(city, SOURCE_LIMIT);
		if (!cleanedCountry.isBlank()) {
			enrichment.setCountry(cleanedCountry);
		}
		if (!cleanedState.isBlank()) {
			enrichment.setState(cleanedState);
		}
		if (!cleanedCity.isBlank()) {
			enrichment.setCity(cleanedCity);
		}
		return enrichmentRepository.save(enrichment);
	}

	public List<NewsAiEnrichment> discoverFromProviders(String query, String country, String state, String city, int limit) {
		String cleanedQuery = query == null ? "" : query.trim();
		if (cleanedQuery.isBlank()) {
			return List.of();
		}

		int safeNewsApiLimit = Math.max(1, Math.min(limit, NEWSAPI_PAGE_SIZE));
		int safeGNewsLimit = Math.max(1, Math.min(limit, GNEWS_PAGE_SIZE));
		Set<String> seenUrls = new LinkedHashSet<>();
		Set<Long> candidateArticleIds = new LinkedHashSet<>();
		SyncCounter counter = new SyncCounter();

		try {
			JsonNode newsApiArticles = readArticles(newsApiClient.fetchDiscoveryNews(cleanedQuery, safeNewsApiLimit),
					"NewsAPI discovery response");
			saveRawArticles(newsApiArticles, "NewsAPI", "urlToImage", seenUrls, candidateArticleIds, counter);
		} catch (Exception e) {
			logger.warn("NewsAPI discovery search failed for query={}", cleanedQuery, e);
		}

		try {
			String gNewsResponse = gNewsClient.fetchDiscoveryNews(cleanedQuery, toGNewsCountryCode(country), safeGNewsLimit);
			syncGNewsResponse(gNewsResponse, seenUrls, candidateArticleIds, counter);
		} catch (Exception e) {
			logger.warn("GNews discovery search failed for query={}", cleanedQuery, e);
		}

		enrichArticles(candidateArticleIds, counter, false);
		if (candidateArticleIds.isEmpty()) {
			return List.of();
		}
		return enrichmentRepository.findByArticleIdsWithArticle(new ArrayList<>(candidateArticleIds))
				.stream()
				.map(enrichment -> applyDiscoveryLocationOverrides(enrichment, country, state, city))
				.limit(Math.max(1, Math.min(limit, 100)))
				.toList();
	}

	private String toGNewsCountryCode(String country) {
		if (country == null || country.isBlank()) {
			return "";
		}
		String normalized = country.trim().toLowerCase();
		return normalized.equals("india") ? "in" : "";
	}
	private void syncNewsApiIndia(int hours, Set<String> seenUrls, Set<Long> candidateArticleIds, SyncCounter counter)
			throws Exception {
		String response = newsApiClient.fetchIndiaNewsSince(hours, NEWSAPI_PAGE_SIZE);
		JsonNode articles = readArticles(response, "NewsAPI India response");

		if (articles.isEmpty()) {
			logger.info("NewsAPI India last-{}h search returned 0 articles. Trying plain India search fallback.", hours);
			response = newsApiClient.fetchIndiaNews(NEWSAPI_PAGE_SIZE);
			articles = readArticles(response, "NewsAPI plain India response");
		}

		if (articles.isEmpty()) {
			logger.info("NewsAPI plain India search returned 0 articles. Trying India top-headlines fallback.");
			response = newsApiClient.fetchIndiaTopHeadlines(NEWSAPI_PAGE_SIZE);
			articles = readArticles(response, "NewsAPI India top-headlines response");
		}

		saveRawArticles(articles, "NewsAPI", "urlToImage", seenUrls, candidateArticleIds, counter);
	}

	private void syncNewsApiWorld(int hours, Set<String> seenUrls, Set<Long> candidateArticleIds, SyncCounter counter)
			throws Exception {
		String response = newsApiClient.fetchWorldNewsSince(hours, NEWSAPI_PAGE_SIZE);
		JsonNode articles = readArticles(response, "NewsAPI World response");

		if (articles.isEmpty()) {
			logger.info("NewsAPI World last-{}h search returned 0 articles. Trying plain World search fallback.", hours);
			response = newsApiClient.fetchWorldNews(NEWSAPI_PAGE_SIZE);
			articles = readArticles(response, "NewsAPI plain World response");
		}

		saveRawArticles(articles, "NewsAPI", "urlToImage", seenUrls, candidateArticleIds, counter);
	}

	private void syncGNewsIndia(int hours, Set<String> seenUrls, Set<Long> candidateArticleIds, SyncCounter counter)
			throws Exception {
		syncGNewsResponse(gNewsClient.fetchIndiaNewsSince(hours, GNEWS_PAGE_SIZE), seenUrls, candidateArticleIds, counter);
	}

	private void syncGNewsWorld(int hours, Set<String> seenUrls, Set<Long> candidateArticleIds, SyncCounter counter)
			throws Exception {
		syncGNewsResponse(gNewsClient.fetchWorldNewsSince(hours, GNEWS_PAGE_SIZE), seenUrls, candidateArticleIds, counter);
	}

	private void syncGNewsResponse(String response, Set<String> seenUrls, Set<Long> candidateArticleIds,
			SyncCounter counter) throws Exception {
		if (!gNewsClient.hasApiKey()) {
			logger.info("GNews sync skipped because gnews.api-key is not configured.");
			return;
		}

		if (response == null || response.isBlank()) {
			return;
		}

		JsonNode articles = objectMapper.readTree(response).path("articles");
		if (!articles.isArray()) {
			logger.warn("GNews response did not contain an articles list.");
			return;
		}

		saveRawArticles(articles, "GNews", "image", seenUrls, candidateArticleIds, counter);
	}

	private JsonNode readArticles(String response, String responseName) throws Exception {
		JsonNode articles = objectMapper.readTree(response).path("articles");
		if (!articles.isArray()) {
			throw new IllegalStateException(responseName + " did not contain an articles list.");
		}
		return articles;
	}

	private void saveRawArticles(JsonNode articles, String provider, String imageField, Set<String> seenUrls,
			Set<Long> candidateArticleIds, SyncCounter counter) {
		counter.fetched += articles.size();
		if ("NewsAPI".equals(provider)) {
			counter.newsApiFetched += articles.size();
		} else if ("GNews".equals(provider)) {
			counter.gNewsFetched += articles.size();
		}
		for (JsonNode article : articles) {
			saveRawArticle(article, provider, imageField, seenUrls, candidateArticleIds, counter);
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

	private void enrichArticles(Set<Long> candidateArticleIds, SyncCounter counter, boolean allowPause) {
		if (candidateArticleIds.isEmpty()) {
			return;
		}

		List<Long> unenrichedIds = enrichmentRepository
				.findArticleIdsWithoutEnrichment(new ArrayList<>(candidateArticleIds));
		List<Long> selectedIds = candidateArticleIds.stream()
				.filter(unenrichedIds::contains)
				.toList();
		List<NewsArticle> articles = articleRepository.findByIdIn(selectedIds);
		List<NewsArticle> ordered = new ArrayList<>();
		for (Long id : selectedIds) {
			articles.stream().filter(a -> a.getId().equals(id)).findFirst().ifPresent(ordered::add);
		}

		for (NewsArticle article : ordered) {
			if (allowPause) {
				ensureBatchNotPaused();
			}
			try {
				EnrichmentResult result = enrichArticleWithRetry(article, allowPause);
				saveEnrichment(article, result);
				counter.enriched++;
			} catch (BatchPausedException e) {
				throw e;
			} catch (Exception e) {
				logger.warn("Enrichment failed for article id={}", article.getId(), e);
				counter.enrichmentSkipped++;
			}
		}

	}

	private void ensureBatchNotPaused() {
		batchPauseService.throwIfStopRequested();
	}

	private EnrichmentResult enrichArticleWithRetry(NewsArticle article) throws Exception {
		return enrichArticleWithRetry(article, false);
	}

	private EnrichmentResult enrichArticleWithRetry(NewsArticle article, boolean allowPause) throws Exception {
		int attempt = 0;
		int safeRetryCount = Math.max(1, enrichmentRetryCount);
		while (attempt < safeRetryCount) {
			if (allowPause) {
				ensureBatchNotPaused();
			}
			try {
				return enrichArticleWithGroq(article);
			} catch (HttpClientErrorException.TooManyRequests e) {
				attempt++;
				long waitMs = parseRetryMillis(e.getResponseBodyAsString(), attempt);
				logger.warn("Groq rate limited for article id={}, attempt={}/{}. Waiting {} ms.",
						article.getId(), attempt, safeRetryCount, waitMs);
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
				- Decide one best category naturally. Return exactly one category value. Never return multiple categories, combined categories, comma-separated categories, slash-separated categories, or values like "Business/Economy". Pick the single best category, for example "Business" or "Economy", not both.
				- goodHeadline must be concise, clean and human-friendly.
				- briefStory must be 1-3 sentences.
				- importanceScore must be an integer from 0 to 100.
				- Location rules are strict:
				  1. Never return "Unknown", "Not found", "N/A", "None", or similar placeholder text for country/state/city.
				  2. Return at most one country, one state, and one city. Never return comma-separated or slash-separated location lists.
				  3. If multiple locations are mentioned, choose the most central/relevant location for the article.
				  4. Country, state, and city must form a real geographic hierarchy. The state must belong to the country, and the city must belong to that state.
				  5. Double-check that the state/city you provide are actually inside the country. Double hard-check that the city is actually inside the state.
				  6. Never mix locations from different countries, such as using a foreign state or city under another country.
				  7. If city is present, state and country must also be present.
				  8. If city is absent but state is present, country must be present.
				  9. If city and state are both absent, country must still be present.
				  10. If the exact city or state cannot be confidently verified as belonging to the selected country/state, return an empty string for that field, but still infer the most likely country from title/description/content/source/url.
				  11. Do not put a city name in state, and do not put a state name in city.
				- Use all raw inputs, especially source and URL domain, to infer country when article text is limited.
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
		result.category = normalizeSingleValue(trimToLength(textValue(root, "category"), 255));
		result.goodHeadline = trimToLength(textValue(root, "goodHeadline"), TITLE_LIMIT);
		result.briefStory = trimToLength(textValue(root, "briefStory"), CONTENT_LIMIT);
		result.country = normalizeSingleValue(trimToLength(textValue(root, "country"), 255));
		result.state = normalizeSingleValue(trimToLength(textValue(root, "state"), 255));
		result.city = normalizeSingleValue(trimToLength(textValue(root, "city"), 255));

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
		normalizeLocation(result);
		return result;
	}

	private String normalizeSingleValue(String value) {
		if (value == null) {
			return "";
		}
		String cleaned = value.trim();
		int comma = cleaned.indexOf(',');
		if (comma > 0) {
			cleaned = cleaned.substring(0, comma).trim();
		}
		int semicolon = cleaned.indexOf(';');
		if (semicolon > 0) {
			cleaned = cleaned.substring(0, semicolon).trim();
		}
		int slash = cleaned.indexOf('/');
		if (slash > 0) {
			cleaned = cleaned.substring(0, slash).trim();
		}
		return cleaned;
	}
	private void normalizeLocation(EnrichmentResult result) {
		result.country = normalizeCountry(result.country);
		result.state = normalizeOptionalLocation(result.state);
		result.city = normalizeOptionalLocation(result.city);

		if (!result.city.isBlank() && result.state.isBlank()) {
			result.city = "";
		}
	}

	private String normalizeCountry(String country) {
		String value = normalizeOptionalLocation(country);
		return value.isBlank() ? "World" : value;
	}

	private String normalizeOptionalLocation(String value) {
		String cleaned = value == null ? "" : value.trim();
		if (cleaned.isBlank()) {
			return "";
		}
		String lower = cleaned.toLowerCase();
		if (lower.equals("unknown") || lower.equals("not found") || lower.equals("n/a") || lower.equals("na")
				|| lower.equals("none") || lower.equals("null") || lower.equals("not specified")
				|| lower.equals("not available")) {
			return "";
		}
		return cleaned;
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
		return Math.max(1L, enrichmentRetryBaseDelayMs) * attempt;
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









