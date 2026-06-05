package com.newsai.news_ai_backend.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.newsai.news_ai_backend.model.NewsAiEnrichment;
import com.newsai.news_ai_backend.model.NewsArticle;
import com.newsai.news_ai_backend.model.NewsDiscoverRequest;
import com.newsai.news_ai_backend.model.NewsDiscoverResult;
import com.newsai.news_ai_backend.repository.NewsArticleRepository;
import com.newsai.news_ai_backend.repository.NewsDiscoverRequestRepository;
import com.newsai.news_ai_backend.repository.NewsDiscoverResultRepository;

@Service
public class NewsDiscoveryBackgroundService {

	private static final Logger logger = LoggerFactory.getLogger(NewsDiscoveryBackgroundService.class);
	private static final int BACKGROUND_DISCOVER_LIMIT = 30;

	private final NewsDiscoverRequestRepository discoverRequestRepository;
	private final NewsDiscoverResultRepository discoverResultRepository;
	private final NewsArticleRepository articleRepository;
	private final NewsSyncService newsSyncService;

	public NewsDiscoveryBackgroundService(NewsDiscoverRequestRepository discoverRequestRepository,
			NewsDiscoverResultRepository discoverResultRepository, NewsArticleRepository articleRepository,
			NewsSyncService newsSyncService) {
		this.discoverRequestRepository = discoverRequestRepository;
		this.discoverResultRepository = discoverResultRepository;
		this.articleRepository = articleRepository;
		this.newsSyncService = newsSyncService;
	}

	@Async
	public void processDiscoverRequest(Long requestId) {
		NewsDiscoverRequest request = discoverRequestRepository.findById(requestId).orElse(null);
		if (request == null) {
			return;
		}
		try {
			int displayOrder = nextDisplayOrder(request.getId());
			List<NewsArticle> candidates = new ArrayList<>();
			candidates.addAll(articleRepository.findRawMatches(request.getContext(), org.springframework.data.domain.PageRequest.of(0, BACKGROUND_DISCOVER_LIMIT)));
			candidates.addAll(newsSyncService.fetchDiscoveryCandidateArticles(request.getProviderQuery(), request.getCountry(), BACKGROUND_DISCOVER_LIMIT));

			for (NewsArticle article : candidates) {
				NewsAiEnrichment enrichment = newsSyncService
						.enrichArticleForDiscovery(article, request.getCountry(), request.getState(), request.getCity())
						.orElse(null);
				if (enrichment != null) {
					displayOrder = saveResultIfNeeded(request, enrichment.getNewsArticle(), displayOrder);
				}
			}
			request.setStatus("COMPLETED");
			request.setUpdatedAt(LocalDateTime.now());
			discoverRequestRepository.save(request);
		} catch (Exception e) {
			logger.warn("Discover background processing failed for requestKey={}", request.getRequestKey(), e);
			request.setStatus("FAILED");
			request.setUpdatedAt(LocalDateTime.now());
			discoverRequestRepository.save(request);
		}
	}

	private int saveResultIfNeeded(NewsDiscoverRequest request, NewsArticle article, int displayOrder) {
		if (article == null || article.getId() == null) {
			return displayOrder;
		}
		if (discoverResultRepository.findByDiscoverRequestIdAndNewsArticleId(request.getId(), article.getId()).isPresent()) {
			return displayOrder;
		}
		NewsDiscoverResult result = new NewsDiscoverResult();
		result.setDiscoverRequest(request);
		result.setNewsArticle(article);
		result.setDisplayOrder(displayOrder);
		result.setSentToUser(false);
		result.setCreatedAt(LocalDateTime.now());
		discoverResultRepository.save(result);
		return displayOrder + 1;
	}

	private int nextDisplayOrder(Long requestId) {
		Integer maxDisplayOrder = discoverResultRepository.findMaxDisplayOrder(requestId);
		return maxDisplayOrder == null ? 1 : maxDisplayOrder + 1;
	}
}