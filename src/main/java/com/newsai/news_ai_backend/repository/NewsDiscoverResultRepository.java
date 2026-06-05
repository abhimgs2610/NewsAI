package com.newsai.news_ai_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.newsai.news_ai_backend.model.NewsDiscoverResult;

public interface NewsDiscoverResultRepository extends JpaRepository<NewsDiscoverResult, Long> {

	Optional<NewsDiscoverResult> findByDiscoverRequestIdAndNewsArticleId(Long discoverRequestId, Long newsArticleId);

	@Query("""
			SELECT r
			FROM NewsDiscoverResult r
			JOIN FETCH r.newsArticle a
			WHERE r.discoverRequest.id = :requestId
			  AND r.sentToUser = false
			ORDER BY r.displayOrder ASC, r.id ASC
			""")
	List<NewsDiscoverResult> findReadyUnsent(@Param("requestId") Long requestId, Pageable pageable);

	@Query("""
			SELECT MAX(r.displayOrder)
			FROM NewsDiscoverResult r
			WHERE r.discoverRequest.id = :requestId
			""")
	Integer findMaxDisplayOrder(@Param("requestId") Long requestId);

	long countByDiscoverRequestIdAndSentToUserFalse(Long discoverRequestId);
}