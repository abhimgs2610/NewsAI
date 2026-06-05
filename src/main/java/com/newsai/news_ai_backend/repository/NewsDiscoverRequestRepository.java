package com.newsai.news_ai_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.newsai.news_ai_backend.model.NewsDiscoverRequest;

public interface NewsDiscoverRequestRepository extends JpaRepository<NewsDiscoverRequest, Long> {

	Optional<NewsDiscoverRequest> findByRequestKey(String requestKey);
}