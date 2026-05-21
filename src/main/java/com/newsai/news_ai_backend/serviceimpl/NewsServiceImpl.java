package com.newsai.news_ai_backend.serviceimpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.newsai.news_ai_backend.dto.NewsFeedDto;
import com.newsai.news_ai_backend.dto.StoryResponseDto;
import com.newsai.news_ai_backend.model.NewsAiEnrichment;
import com.newsai.news_ai_backend.model.NewsArticle;
import com.newsai.news_ai_backend.model.NewsStory;
import com.newsai.news_ai_backend.model.StoryLanguage;
import com.newsai.news_ai_backend.repository.NewsAiEnrichmentRepository;
import com.newsai.news_ai_backend.repository.NewsArticleRepository;
import com.newsai.news_ai_backend.repository.NewsStoryRepository;
import com.newsai.news_ai_backend.service.NewsService;
import com.newsai.news_ai_backend.service.StoryGenerator;

@Service
public class NewsServiceImpl implements NewsService {

	private static final int STORY_LIMIT = 60000;

	private final NewsAiEnrichmentRepository enrichmentRepository;
	private final NewsArticleRepository articleRepository;
	private final NewsStoryRepository storyRepository;
	private final StoryGenerator storyGenerator;
	private final ArticleContentExtractor articleContentExtractor;

	public NewsServiceImpl(NewsAiEnrichmentRepository enrichmentRepository, NewsArticleRepository articleRepository,
			NewsStoryRepository storyRepository, StoryGenerator storyGenerator,
			ArticleContentExtractor articleContentExtractor) {
		this.enrichmentRepository = enrichmentRepository;
		this.articleRepository = articleRepository;
		this.storyRepository = storyRepository;
		this.storyGenerator = storyGenerator;
		this.articleContentExtractor = articleContentExtractor;
	}

	@Override
	public List<NewsFeedDto> getFeed(String category, String state, String city, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 100));
		return enrichmentRepository.findFeed(clean(category), clean(state), clean(city), PageRequest.of(0, safeLimit))
				.stream()
				.map(this::toFeedDto)
				.toList();
	}

	@Override
	public List<NewsFeedDto> getHotNews(int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 100));
		return enrichmentRepository.findHot(PageRequest.of(0, safeLimit))
				.stream()
				.map(this::toFeedDto)
				.toList();
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
		String extracted = "";
		if (article.getUrl() != null && !article.getUrl().isBlank()) {
			extracted = articleContentExtractor.extract(article.getUrl());
		}

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
