package com.newsai.news_ai_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.newsai.news_ai_backend.model.NewsArticle;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

	Optional<NewsArticle> findByUrl(String url);

	List<NewsArticle> findByIdIn(List<Long> ids);

	List<NewsArticle> findAllByOrderByPublishedAtDescIdDesc(Pageable pageable);
	@Query("""
			SELECT a
			FROM NewsArticle a
			WHERE (:query IS NULL OR :query = '' OR
			       LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.source) LIKE LOWER(CONCAT('%', :query, '%')))
			ORDER BY a.publishedAt DESC, a.id DESC
			""")
	List<NewsArticle> findRawMatches(@Param("query") String query, Pageable pageable);
}
