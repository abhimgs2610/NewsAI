package com.newsai.news_ai_backend.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.newsai.news_ai_backend.client.OpenAiClient;
import com.newsai.news_ai_backend.dto.CountryCountDto;
import com.newsai.news_ai_backend.dto.NewsAskRequestDto;
import com.newsai.news_ai_backend.dto.NewsAskResponseDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryRequestDto;
import com.newsai.news_ai_backend.dto.NewsDiscoveryResponseDto;
import com.newsai.news_ai_backend.dto.NewsFeedDto;
import com.newsai.news_ai_backend.dto.StoryResponseDto;
import com.newsai.news_ai_backend.model.NewsAiEnrichment;
import com.newsai.news_ai_backend.model.NewsArticle;
import com.newsai.news_ai_backend.pagination.OffsetBasedPageRequest;
import com.newsai.news_ai_backend.model.NewsChatMessage;
import com.newsai.news_ai_backend.model.NewsDiscoverRequest;
import com.newsai.news_ai_backend.model.NewsDiscoverResult;
import com.newsai.news_ai_backend.model.NewsStory;
import com.newsai.news_ai_backend.model.StoryLanguage;
import com.newsai.news_ai_backend.repository.NewsAiEnrichmentRepository;
import com.newsai.news_ai_backend.repository.NewsArticleRepository;
import com.newsai.news_ai_backend.repository.NewsChatMessageRepository;
import com.newsai.news_ai_backend.repository.NewsDiscoverRequestRepository;
import com.newsai.news_ai_backend.repository.NewsDiscoverResultRepository;
import com.newsai.news_ai_backend.repository.NewsStoryRepository;
import com.newsai.news_ai_backend.service.NewsService;
import com.newsai.news_ai_backend.service.StoryGenerator;

@Service
public class NewsServiceImpl implements NewsService {

	private static final int STORY_LIMIT = 60000;
	private static final int FOLLOW_UP_EXTRACTED_LIMIT = 12000;
	private static final int FOLLOW_UP_STORY_LIMIT = 12000;
	private static final int FOLLOW_UP_ANSWER_LIMIT = 12000;
	private static final int FOLLOW_UP_HISTORY_LIMIT = 12000;
	private static final int FOLLOW_UP_HISTORY_ENTRY_LIMIT = 2000;
	private static final int ARTICLE_CONTEXT_FALLBACK_LIMIT = 12000;
	private static final int DISCOVER_PAGE_SIZE = 5;
	private static final long DISCOVER_INITIAL_WAIT_MS = 5000L;
	private static final long DISCOVER_INITIAL_WAIT_POLL_MS = 250L;

	private final NewsAiEnrichmentRepository enrichmentRepository;
	private final NewsArticleRepository articleRepository;
	private final NewsStoryRepository storyRepository;
	private final NewsChatMessageRepository chatMessageRepository;
	private final NewsDiscoverRequestRepository discoverRequestRepository;
	private final NewsDiscoverResultRepository discoverResultRepository;
	private final StoryGenerator storyGenerator;
	private final ArticleContentExtractor articleContentExtractor;
	private final NewsSyncService newsSyncService;
	private final NewsDiscoveryBackgroundService discoveryBackgroundService;
	private final OpenAiClient openAiClient;

	public NewsServiceImpl(NewsAiEnrichmentRepository enrichmentRepository, NewsArticleRepository articleRepository,
			NewsStoryRepository storyRepository, NewsChatMessageRepository chatMessageRepository,
			NewsDiscoverRequestRepository discoverRequestRepository, NewsDiscoverResultRepository discoverResultRepository,
			StoryGenerator storyGenerator, ArticleContentExtractor articleContentExtractor, NewsSyncService newsSyncService,
			NewsDiscoveryBackgroundService discoveryBackgroundService, OpenAiClient openAiClient) {
		this.enrichmentRepository = enrichmentRepository;
		this.articleRepository = articleRepository;
		this.storyRepository = storyRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.discoverRequestRepository = discoverRequestRepository;
		this.discoverResultRepository = discoverResultRepository;
		this.storyGenerator = storyGenerator;
		this.articleContentExtractor = articleContentExtractor;
		this.newsSyncService = newsSyncService;
		this.discoveryBackgroundService = discoveryBackgroundService;
		this.openAiClient = openAiClient;
	}

	@Override
	public List<NewsFeedDto> getFeed(String country, String category, String state, String city, String query, int limit, int offset) {
		int safeLimit = Math.max(1, Math.min(limit, 100));
		int safeOffset = Math.max(0, offset);
		return enrichmentRepository.findFeed(clean(country), clean(category), clean(state), clean(city), clean(query),
				new OffsetBasedPageRequest(safeLimit, safeOffset))
				.stream()
				.map(this::toFeedDto)
				.toList();
	}

	@Override
	public NewsDiscoveryResponseDto discoverNews(NewsDiscoveryRequestDto request) {
		if (request == null) {
			throw new IllegalArgumentException("request body is required");
		}
		if (request.isLoadMore()) {
			return loadMoreDiscoverResults(request);
		}
		return startDiscoverRequest(request);
	}

	private NewsDiscoveryResponseDto startDiscoverRequest(NewsDiscoveryRequestDto request) {
		String context = clean(request.getContext());
		if (context.isBlank()) {
			throw new IllegalArgumentException("context is required");
		}

		String country = clean(request.getCountry());
		String state = clean(request.getState());
		String city = clean(request.getCity());
		String providerQuery = buildProviderQuery(city, state, country, context);

		NewsDiscoverRequest discoverRequest = new NewsDiscoverRequest();
		discoverRequest.setRequestKey("DISC-" + UUID.randomUUID());
		discoverRequest.setContext(context);
		discoverRequest.setCountry(country);
		discoverRequest.setState(state);
		discoverRequest.setCity(city);
		discoverRequest.setProviderQuery(providerQuery);
		discoverRequest.setStatus("PROCESSING");
		discoverRequest.setCreatedAt(LocalDateTime.now());
		discoverRequest.setUpdatedAt(LocalDateTime.now());
		discoverRequest = discoverRequestRepository.save(discoverRequest);

		List<NewsAiEnrichment> localMatches = enrichmentRepository
				.findFeed(country, "", state, city, context, PageRequest.of(0, DISCOVER_PAGE_SIZE))
				.stream()
				.map(enrichment -> newsSyncService.applyDiscoveryLocationOverrides(enrichment, country, state, city))
				.toList();
		saveDiscoverResults(discoverRequest, localMatches, 1);

		discoveryBackgroundService.processDiscoverRequest(discoverRequest.getId());
		waitForInitialDiscoverResults(discoverRequest.getId());
		discoverRequest = discoverRequestRepository.findById(discoverRequest.getId()).orElse(discoverRequest);
		return buildDiscoverResponse(discoverRequest, discoverRequest.getStatus());
	}

	private void waitForInitialDiscoverResults(Long discoverRequestId) {
		long deadline = System.currentTimeMillis() + DISCOVER_INITIAL_WAIT_MS;
		while (System.currentTimeMillis() < deadline) {
			long readyCount = discoverResultRepository.countByDiscoverRequestIdAndSentToUserFalse(discoverRequestId);
			if (readyCount >= DISCOVER_PAGE_SIZE) {
				return;
			}
			String status = discoverRequestRepository.findById(discoverRequestId)
					.map(NewsDiscoverRequest::getStatus)
					.orElse("COMPLETED");
			if (!"PROCESSING".equalsIgnoreCase(status)) {
				return;
			}
			try {
				Thread.sleep(DISCOVER_INITIAL_WAIT_POLL_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
	private NewsDiscoveryResponseDto loadMoreDiscoverResults(NewsDiscoveryRequestDto request) {
		String requestKey = clean(request.getDiscoverRequestId());
		if (requestKey.isBlank()) {
			throw new IllegalArgumentException("discoverRequestId is required for loadMore");
		}
		NewsDiscoverRequest discoverRequest = discoverRequestRepository.findByRequestKey(requestKey)
				.orElseThrow(() -> new IllegalArgumentException("discoverRequestId is invalid"));
		return buildDiscoverResponse(discoverRequest, discoverRequest.getStatus());
	}

	private void saveDiscoverResults(NewsDiscoverRequest request, List<NewsAiEnrichment> enrichments, int startOrder) {
		int displayOrder = startOrder;
		for (NewsAiEnrichment enrichment : enrichments) {
			NewsArticle article = enrichment.getNewsArticle();
			if (article == null || article.getId() == null) {
				continue;
			}
			if (discoverResultRepository.findByDiscoverRequestIdAndNewsArticleId(request.getId(), article.getId()).isPresent()) {
				continue;
			}
			NewsDiscoverResult result = new NewsDiscoverResult();
			result.setDiscoverRequest(request);
			result.setNewsArticle(article);
			result.setDisplayOrder(displayOrder++);
			result.setSentToUser(false);
			result.setCreatedAt(LocalDateTime.now());
			discoverResultRepository.save(result);
		}
	}

	private NewsDiscoveryResponseDto buildDiscoverResponse(NewsDiscoverRequest discoverRequest, String status) {
		List<NewsDiscoverResult> readyResults = discoverResultRepository
				.findReadyUnsent(discoverRequest.getId(), PageRequest.of(0, DISCOVER_PAGE_SIZE));
		List<Long> articleIds = readyResults.stream()
				.map(result -> result.getNewsArticle().getId())
				.toList();
		List<NewsAiEnrichment> enrichments = articleIds.isEmpty() ? List.of()
				: enrichmentRepository.findByArticleIdsWithArticle(articleIds);
		List<NewsFeedDto> feedResults = articleIds.stream()
				.map(articleId -> enrichments.stream()
						.filter(enrichment -> enrichment.getNewsArticle().getId().equals(articleId))
						.findFirst()
						.orElse(null))
				.filter(enrichment -> enrichment != null)
				.map(this::toFeedDto)
				.toList();
		readyResults.forEach(result -> result.setSentToUser(true));
		discoverResultRepository.saveAll(readyResults);

		long remainingReady = discoverResultRepository.countByDiscoverRequestIdAndSentToUserFalse(discoverRequest.getId());
		String finalStatus = clean(status).isBlank() ? "PROCESSING" : status;
		boolean hasMore = remainingReady > 0 || "PROCESSING".equalsIgnoreCase(finalStatus);
		String message = discoverMessage(finalStatus, feedResults.size());

		NewsDiscoveryResponseDto response = new NewsDiscoveryResponseDto(finalStatus, message,
				discoverRequest.getProviderQuery(), feedResults);
		response.setDiscoverRequestId(discoverRequest.getRequestKey());
		response.setHasMore(hasMore);
		response.setReadyCount(feedResults.size());
		return response;
	}

	private String discoverMessage(String status, int resultCount) {
		if (resultCount > 0) {
			return "Records fetched successfully";
		}
		if ("PROCESSING".equalsIgnoreCase(status)) {
			return "Processing is still in progress.";
		}
		if ("FAILED".equalsIgnoreCase(status)) {
			return "Discover processing failed. Please try again.";
		}
		return "No more matching news found.";
	}

	@Override
	public List<NewsFeedDto> getHotNews(String query, int limit, int offset) {
		int safeLimit = Math.max(1, Math.min(limit, 100));
		int safeOffset = Math.max(0, offset);
		return enrichmentRepository.findHot(clean(query), new OffsetBasedPageRequest(safeLimit, safeOffset))
				.stream()
				.map(this::toFeedDto)
				.toList();
	}

	@Override
	public List<CountryCountDto> getCountries() {
		return enrichmentRepository.findCountryCounts()
				.stream()
				.map(row -> new CountryCountDto((String) row[0], ((Number) row[1]).longValue()))
				.toList();
	}

	@Override
	public List<String> getCategories() {
		return enrichmentRepository.findDistinctIndiaCategories();
	}

	@Override
	public List<String> getStates() {
		return enrichmentRepository.findDistinctIndiaStates();
	}

	@Override
	public List<String> getCities(String state) {
		return enrichmentRepository.findDistinctIndiaCities(clean(state));
	}

	@Override
	public StoryResponseDto getStoryById(Long id, String language, String style, boolean refresh) {
		StoryLanguage selectedLanguage = StoryLanguage.fromInput(language);
		String selectedStyle = style == null || style.isBlank() ? "genz" : style.trim().toLowerCase();

		NewsArticle article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("News not found"));

		if (!refresh) {
			return storyRepository
					.findByNewsArticleIdAndStyleAndLanguage(id, selectedStyle, selectedLanguage.name())
					.map(existing -> new StoryResponseDto(existing.getStory()))
					.orElseGet(() -> createAndSaveStory(article, selectedLanguage, selectedStyle));
		}

		return createAndSaveStory(article, selectedLanguage, selectedStyle);
	}

	@Override
	public NewsAskResponseDto askNewsQuestion(Long id, NewsAskRequestDto request) {
		String question = clean(request == null ? "" : request.getQuestion());
		if (question.isBlank()) {
			throw new IllegalArgumentException("question is required");
		}

		StoryLanguage selectedLanguage = StoryLanguage.fromInput(request == null ? "" : request.getLanguage());
		String selectedStyle = "genz";
		NewsArticle article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("News not found"));
		NewsAiEnrichment enrichment = enrichmentRepository.findByNewsArticleId(article.getId()).orElse(null);
		String extractedContent = getOrExtractArticleContent(article);
		String story = storyRepository
				.findByNewsArticleIdAndStyleAndLanguage(article.getId(), selectedStyle, selectedLanguage.name())
				.map(NewsStory::getStory)
				.orElseGet(() -> createAndSaveStory(article, selectedLanguage, selectedStyle).getStory());
		List<NewsChatMessage> previousMessages = chatMessageRepository.findByNewsArticleIdOrderByAskedAtAsc(article.getId());

		String prompt = buildFollowUpPrompt(article, enrichment, extractedContent, story, previousMessages, question,
				selectedLanguage.name());
		String answer = trimToLength(openAiClient.generateFollowUpAnswer(prompt), FOLLOW_UP_ANSWER_LIMIT);
		saveChatMessage(article, question, answer);
		return new NewsAskResponseDto(answer);
	}

	private StoryResponseDto createAndSaveStory(NewsArticle article, StoryLanguage language, String style) {
		NewsAiEnrichment enrichment = enrichmentRepository.findByNewsArticleId(article.getId()).orElse(null);
		String title = firstNonBlank(enrichment == null ? "" : enrichment.getGoodHeadline(), article.getTitle());
		String category = firstNonBlank(enrichment == null ? "" : enrichment.getCategory(), "general");
		String context = buildStoryContext(article, enrichment);
		String storyText = trimToLength(
				storyGenerator.buildStory(title, context, category, style, language.name()), STORY_LIMIT);

		NewsStory storyEntity = storyRepository
				.findByNewsArticleIdAndStyleAndLanguage(article.getId(), style, language.name())
				.orElseGet(NewsStory::new);
		storyEntity.setNewsArticle(article);
		storyEntity.setStyle(style);
		storyEntity.setLanguage(language.name());
		storyEntity.setStory(storyText);
		storyEntity.setGeneratedAt(LocalDateTime.now());
		storyRepository.save(storyEntity);

		return new StoryResponseDto(storyText);
	}

	private String buildStoryContext(NewsArticle article, NewsAiEnrichment enrichment) {
		String extracted = getOrExtractArticleContent(article);

		StringBuilder context = new StringBuilder();
		context.append("Source: ").append(safe(article.getSource())).append("\n");
		context.append("URL: ").append(safe(article.getUrl())).append("\n");
		if (enrichment != null) {
			context.append("Category: ").append(safe(enrichment.getCategory())).append("\n");
			context.append("Brief story: ").append(safe(enrichment.getBriefStory())).append("\n");
			context.append("Importance: ").append(enrichment.getImportanceScore() == null ? "" : enrichment.getImportanceScore())
					.append("\n");
		}
		context.append("Description: ").append(safe(article.getDescription())).append("\n");
		context.append("Content: ").append(safe(article.getContent())).append("\n");
		if (!extracted.isBlank()) {
			context.append("Extracted article text: ").append(extracted);
		}
		return context.toString();
	}

	private String getOrExtractArticleContent(NewsArticle article) {
		if (article.getExtractedContent() != null && !article.getExtractedContent().isBlank()) {
			return article.getExtractedContent();
		}
		String fallbackContent = buildArticleContextFallback(article);
		if (article.getUrl() == null || article.getUrl().isBlank()) {
			return fallbackContent;
		}
		String extracted = articleContentExtractor.extract(article.getUrl());
		String contentToSave = extracted.isBlank() ? fallbackContent : extracted;
		if (!contentToSave.isBlank()) {
			article.setExtractedContent(contentToSave);
			article.setExtractedAt(LocalDateTime.now());
			articleRepository.save(article);
		}
		return contentToSave;
	}

	private String buildArticleContextFallback(NewsArticle article) {
		StringBuilder fallback = new StringBuilder();
		appendContextField(fallback, "Title", article.getTitle());
		appendContextField(fallback, "Description", article.getDescription());
		appendContextField(fallback, "Content", article.getContent());
		return trimToLength(fallback.toString(), ARTICLE_CONTEXT_FALLBACK_LIMIT);
	}

	private void appendContextField(StringBuilder builder, String label, String value) {
		String cleaned = safe(value);
		if (!cleaned.isBlank()) {
			builder.append(label).append(": ").append(cleaned).append("\n");
		}
	}

	private String buildFollowUpPrompt(NewsArticle article, NewsAiEnrichment enrichment, String extractedContent,
			String story, List<NewsChatMessage> previousMessages, String question, String language) {
		String title = firstNonBlank(enrichment == null ? "" : enrichment.getGoodHeadline(), article.getTitle());
		String category = firstNonBlank(enrichment == null ? "" : enrichment.getCategory(), "general");
		String previousConversation = buildPreviousConversation(previousMessages);

		String prompt = "You are NewsAI, answering a follow-up question in details not just brief short about a news.\n\n"
				+ "Use the provided news article fields, extracted article content, and generated NewsAI story as context. "
				+ "You may use your general knowledge carefully when the question needs background context, but do not pretend the article said something it did not say.\n\n"
				+ "Answer in a human, conversational NewsAI style. Be clear, detailed, and easy to understand. "
				+ "If the topic is serious or sensitive, reduce jokes and slang.\n\n"
				+ "Generate answer language strictly in: " + language + "\n"
				+ "User question:\n" + question + "\n\n"
				+ "News title:\n" + safe(title) + "\n\n"
				+ "Source:\n" + safe(article.getSource()) + "\n\n"
				+ "URL:\n" + safe(article.getUrl()) + "\n\n"
				+ "Category:\n" + safe(category) + "\n\n"
				+ "Raw article description:\n" + safe(article.getDescription()) + "\n\n"
				+ "Raw article content:\n" + safe(article.getContent()) + "\n\n"
				+ "Extracted or fallback article context:\n" + trimToLength(safe(extractedContent), FOLLOW_UP_EXTRACTED_LIMIT) + "\n\n"
				+ "Generated NewsAI story:\n" + trimToLength(safe(story), FOLLOW_UP_STORY_LIMIT) + "\n\n";
		if (!previousConversation.isBlank()) {
			prompt += "Previously asked questions and answers for this same news:\n" + previousConversation + "\n\n"
					+ "Use this previous chat history so follow-up words like then, that, after that, or why make sense.\n\n";
		}
		return prompt + "Answer the user's question now.";
	}

	private String buildPreviousConversation(List<NewsChatMessage> previousMessages) {
		if (previousMessages == null || previousMessages.isEmpty()) {
			return "";
		}
		StringBuilder history = new StringBuilder();
		int turn = 1;
		for (NewsChatMessage message : previousMessages) {
			String entry = "Q" + turn + ": " + safe(message.getQuestion()) + "\n"
					+ "A" + turn + ": " + safe(message.getAnswer()) + "\n";
			entry = trimToLength(entry, FOLLOW_UP_HISTORY_ENTRY_LIMIT);
			if (history.length() + entry.length() > FOLLOW_UP_HISTORY_LIMIT) {
				break;
			}
			history.append(entry).append("\n");
			turn++;
		}
		return history.toString().trim();
	}

	private void saveChatMessage(NewsArticle article, String question, String answer) {
		NewsChatMessage chatMessage = new NewsChatMessage();
		chatMessage.setNewsArticle(article);
		chatMessage.setQuestion(question);
		chatMessage.setAnswer(answer);
		chatMessage.setAskedAt(LocalDateTime.now());
		chatMessageRepository.save(chatMessage);
	}

	private NewsFeedDto toFeedDto(NewsAiEnrichment enrichment) {
		NewsArticle article = enrichment.getNewsArticle();
		NewsFeedDto dto = new NewsFeedDto();
		dto.setId(article.getId());
		dto.setHeadline(firstNonBlank(enrichment.getGoodHeadline(), article.getTitle()));
		dto.setBriefStory(firstNonBlank(enrichment.getBriefStory(), article.getDescription()));
		dto.setCategory(enrichment.getCategory());
		dto.setSource(article.getSource());
		dto.setImageUrl(article.getImageUrl());
		dto.setCountry(enrichment.getCountry());
		dto.setState(enrichment.getState());
		dto.setCity(enrichment.getCity());
		dto.setPublishedAt(article.getPublishedAt() == null ? "" : article.getPublishedAt().toString());
		return dto;
	}

	private NewsDiscoveryResponseDto discoveryResponse(String status, String message, String providerQuery,
			List<NewsAiEnrichment> enrichments) {
		return new NewsDiscoveryResponseDto(status, message, providerQuery,
				enrichments.stream().map(this::toFeedDto).toList());
	}

	private String buildProviderQuery(String city, String state, String country, String context) {
		List<String> parts = new ArrayList<>();
		addIfPresent(parts, city);
		addIfPresent(parts, state);
		addIfPresent(parts, country);
		addIfPresent(parts, context);
		return String.join(" ", parts);
	}

	private void addIfPresent(List<String> parts, String value) {
		String cleaned = clean(value);
		if (!cleaned.isBlank()) {
			parts.add(cleaned);
		}
	}

	private String clean(String value) {
		return value == null ? "" : value.trim();
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private String firstNonBlank(String first, String fallback) {
		return first == null || first.isBlank() ? (fallback == null ? "" : fallback) : first;
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
}


