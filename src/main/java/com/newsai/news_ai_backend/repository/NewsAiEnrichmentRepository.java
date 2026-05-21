package com.newsai.news_ai_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.newsai.news_ai_backend.model.NewsAiEnrichment;

public interface NewsAiEnrichmentRepository extends JpaRepository<NewsAiEnrichment, Long> {

	Optional<NewsAiEnrichment> findByNewsArticleId(Long newsArticleId);

	@Query("""
			SELECT e
			FROM NewsAiEnrichment e
			JOIN FETCH e.newsArticle a
			WHERE (:category IS NULL OR :category = '' OR LOWER(e.category) = LOWER(:category))
			  AND (:state IS NULL OR :state = '' OR LOWER(e.state) = LOWER(:state))
			  AND (:city IS NULL OR :city = '' OR LOWER(e.city) = LOWER(:city))
			ORDER BY a.publishedAt DESC, a.id DESC
			""")
	List<NewsAiEnrichment> findFeed(@Param("category") String category, @Param("state") String state,
			@Param("city") String city, Pageable pageable);

	@Query("""
			SELECT e
			FROM NewsAiEnrichment e
			JOIN FETCH e.newsArticle a
			ORDER BY e.importanceScore DESC, a.publishedAt DESC, a.id DESC
			""")
	List<NewsAiEnrichment> findHot(Pageable pageable);
}
