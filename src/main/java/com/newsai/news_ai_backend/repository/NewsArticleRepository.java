package com.newsai.news_ai_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.newsai.news_ai_backend.model.NewsArticle;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

	Optional<NewsArticle> findByUrl(String url);

	List<NewsArticle> findByIdIn(List<Long> ids);

	List<NewsArticle> findAllByOrderByPublishedAtDescIdDesc(Pageable pageable);
}
