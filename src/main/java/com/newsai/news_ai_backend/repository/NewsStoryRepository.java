package com.newsai.news_ai_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.newsai.news_ai_backend.model.NewsStory;

public interface NewsStoryRepository extends JpaRepository<NewsStory, Long> {

	Optional<NewsStory> findByNewsArticleIdAndStyleAndLanguage(Long newsArticleId, String style, String language);
}
