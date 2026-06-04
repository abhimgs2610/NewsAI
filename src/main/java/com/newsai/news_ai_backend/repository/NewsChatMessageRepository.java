package com.newsai.news_ai_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.newsai.news_ai_backend.model.NewsChatMessage;

public interface NewsChatMessageRepository extends JpaRepository<NewsChatMessage, Long> {

	List<NewsChatMessage> findByNewsArticleIdOrderByAskedAtAsc(Long newsArticleId);
}
