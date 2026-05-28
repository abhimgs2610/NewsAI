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
			SELECT a.id
			FROM NewsArticle a
			WHERE a.id IN :articleIds
			  AND NOT EXISTS (
			      SELECT 1
			      FROM NewsAiEnrichment e
			      WHERE e.newsArticle.id = a.id
			  )
			""")
	List<Long> findArticleIdsWithoutEnrichment(@Param("articleIds") List<Long> articleIds);

	@Query("""
			SELECT
			  CASE
			    WHEN e.country IS NULL OR TRIM(e.country) = '' OR LOWER(TRIM(e.country)) = 'unknown' THEN 'World'
			    ELSE TRIM(e.country)
			  END,
			  COUNT(e.id)
			FROM NewsAiEnrichment e
			GROUP BY
			  CASE
			    WHEN e.country IS NULL OR TRIM(e.country) = '' OR LOWER(TRIM(e.country)) = 'unknown' THEN 'World'
			    ELSE TRIM(e.country)
			  END
			ORDER BY
			  CASE
			    WHEN e.country IS NULL OR TRIM(e.country) = '' OR LOWER(TRIM(e.country)) = 'unknown' THEN 'World'
			    ELSE TRIM(e.country)
			  END ASC
			""")
	List<Object[]> findCountryCounts();

	@Query("""
			SELECT DISTINCT TRIM(e.category)
			FROM NewsAiEnrichment e
			WHERE e.category IS NOT NULL AND TRIM(e.category) <> ''
			  AND LOWER(TRIM(e.category)) <> 'unknown'
			  AND LOWER(TRIM(e.country)) = 'india'
			ORDER BY TRIM(e.category) ASC
			""")
	List<String> findDistinctIndiaCategories();

	@Query("""
			SELECT DISTINCT TRIM(e.state)
			FROM NewsAiEnrichment e
			WHERE e.state IS NOT NULL AND TRIM(e.state) <> ''
			  AND LOWER(TRIM(e.state)) <> 'unknown'
			  AND LOWER(TRIM(e.country)) = 'india'
			  AND LOWER(TRIM(e.state)) IN (
			      'andhra pradesh', 'arunachal pradesh', 'assam', 'bihar', 'chhattisgarh',
			      'goa', 'gujarat', 'haryana', 'himachal pradesh', 'jharkhand', 'karnataka',
			      'kerala', 'madhya pradesh', 'maharashtra', 'manipur', 'meghalaya', 'mizoram',
			      'nagaland', 'odisha', 'punjab', 'rajasthan', 'sikkim', 'tamil nadu',
			      'telangana', 'tripura', 'uttar pradesh', 'uttarakhand', 'west bengal',
			      'andaman and nicobar islands', 'chandigarh', 'dadra and nagar haveli and daman and diu',
			      'delhi', 'jammu and kashmir', 'ladakh', 'lakshadweep', 'puducherry'
			  )
			ORDER BY TRIM(e.state) ASC
			""")
	List<String> findDistinctIndiaStates();

	@Query("""
			SELECT DISTINCT TRIM(e.city)
			FROM NewsAiEnrichment e
			WHERE e.city IS NOT NULL AND TRIM(e.city) <> ''
			  AND LOWER(TRIM(e.city)) <> 'unknown'
			  AND LOWER(TRIM(e.country)) = 'india'
			  AND (:state IS NULL OR :state = '' OR LOWER(e.state) LIKE LOWER(CONCAT('%', :state, '%')))
			ORDER BY TRIM(e.city) ASC
			""")
	List<String> findDistinctIndiaCities(@Param("state") String state);

	@Query("""
			SELECT e
			FROM NewsAiEnrichment e
			JOIN FETCH e.newsArticle a
			WHERE (:country IS NULL OR :country = '' OR
			       (LOWER(:country) = 'world' AND (e.country IS NULL OR LOWER(e.country) <> 'india')) OR
			       (LOWER(:country) <> 'world' AND LOWER(e.country) LIKE LOWER(CONCAT('%', :country, '%'))))
			  AND (:category IS NULL OR :category = '' OR LOWER(e.category) LIKE LOWER(CONCAT('%', :category, '%')))
			  AND (:state IS NULL OR :state = '' OR LOWER(e.state) LIKE LOWER(CONCAT('%', :state, '%')))
			  AND (:city IS NULL OR :city = '' OR LOWER(e.city) LIKE LOWER(CONCAT('%', :city, '%')))
			  AND (:query IS NULL OR :query = '' OR
			       LOWER(e.goodHeadline) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(e.briefStory) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(e.category) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.source) LIKE LOWER(CONCAT('%', :query, '%')))
			ORDER BY a.publishedAt DESC, a.id DESC
			""")
	List<NewsAiEnrichment> findFeed(@Param("country") String country, @Param("category") String category,
			@Param("state") String state, @Param("city") String city, @Param("query") String query, Pageable pageable);

	@Query("""
			SELECT e
			FROM NewsAiEnrichment e
			JOIN FETCH e.newsArticle a
			WHERE (:query IS NULL OR :query = '' OR
			       LOWER(e.goodHeadline) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(e.briefStory) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(e.category) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR
			       LOWER(a.source) LIKE LOWER(CONCAT('%', :query, '%')))
			ORDER BY e.importanceScore DESC, a.publishedAt DESC, a.id DESC
			""")
	List<NewsAiEnrichment> findHot(@Param("query") String query, Pageable pageable);
	@Query("""
			SELECT e
			FROM NewsAiEnrichment e
			JOIN FETCH e.newsArticle a
			WHERE a.id IN :articleIds
			ORDER BY e.importanceScore DESC, a.publishedAt DESC, a.id DESC
			""")
	List<NewsAiEnrichment> findByArticleIdsWithArticle(@Param("articleIds") List<Long> articleIds);
}
