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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "news_ai_enrichment", indexes = {
		@Index(name = "idx_news_ai_enrichment_category_processed", columnList = "category,processed_at"),
		@Index(name = "idx_news_ai_enrichment_state_city_processed", columnList = "state,city,processed_at"),
		@Index(name = "idx_news_ai_enrichment_importance_processed", columnList = "importance_score,processed_at")
})
public class NewsAiEnrichment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "news_article_id", nullable = false, unique = true)
	private NewsArticle newsArticle;

	@Column(name = "category", length = 255)
	private String category;

	@Column(name = "good_headline", length = 1000)
	private String goodHeadline;

	@Column(name = "brief_story", length = 2000)
	private String briefStory;

	@Column(name = "importance_score")
	private Integer importanceScore;

	@Column(name = "country", length = 255)
	private String country;

	@Column(name = "state", length = 255)
	private String state;

	@Column(name = "city", length = 255)
	private String city;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public NewsArticle getNewsArticle() {
		return newsArticle;
	}

	public void setNewsArticle(NewsArticle newsArticle) {
		this.newsArticle = newsArticle;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getGoodHeadline() {
		return goodHeadline;
	}

	public void setGoodHeadline(String goodHeadline) {
		this.goodHeadline = goodHeadline;
	}

	public String getBriefStory() {
		return briefStory;
	}

	public void setBriefStory(String briefStory) {
		this.briefStory = briefStory;
	}

	public Integer getImportanceScore() {
		return importanceScore;
	}

	public void setImportanceScore(Integer importanceScore) {
		this.importanceScore = importanceScore;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(LocalDateTime processedAt) {
		this.processedAt = processedAt;
	}
}
