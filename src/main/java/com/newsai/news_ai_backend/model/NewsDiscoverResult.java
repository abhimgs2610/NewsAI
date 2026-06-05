package com.newsai.news_ai_backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "news_discover_result", indexes = {
		@Index(name = "idx_news_discover_result_request_sent", columnList = "discover_request_id,sent_to_user,display_order"),
		@Index(name = "idx_news_discover_result_article", columnList = "news_article_id")
}, uniqueConstraints = {
		@UniqueConstraint(name = "uk_news_discover_request_article", columnNames = { "discover_request_id", "news_article_id" })
})
public class NewsDiscoverResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "discover_request_id", nullable = false)
	private NewsDiscoverRequest discoverRequest;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "news_article_id", nullable = false)
	private NewsArticle newsArticle;

	@Column(name = "sent_to_user")
	private boolean sentToUser;

	@Column(name = "display_order")
	private Integer displayOrder;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public NewsDiscoverRequest getDiscoverRequest() {
		return discoverRequest;
	}

	public void setDiscoverRequest(NewsDiscoverRequest discoverRequest) {
		this.discoverRequest = discoverRequest;
	}

	public NewsArticle getNewsArticle() {
		return newsArticle;
	}

	public void setNewsArticle(NewsArticle newsArticle) {
		this.newsArticle = newsArticle;
	}

	public boolean isSentToUser() {
		return sentToUser;
	}

	public void setSentToUser(boolean sentToUser) {
		this.sentToUser = sentToUser;
	}

	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}