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
@Table(name = "news_story", uniqueConstraints = {
		@UniqueConstraint(name = "uk_news_story_article_style_language", columnNames = { "news_article_id", "style",
				"language" })
}, indexes = {
		@Index(name = "idx_news_story_article_style_language", columnList = "news_article_id,style,language")
})
public class NewsStory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "news_article_id", nullable = false)
	private NewsArticle newsArticle;

	@Column(name = "story", columnDefinition = "LONGTEXT")
	private String story;

	@Column(name = "style", length = 255)
	private String style;

	@Column(name = "language", length = 32)
	private String language;

	@Column(name = "generated_at")
	private LocalDateTime generatedAt;

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

	public String getStory() {
		return story;
	}

	public void setStory(String story) {
		this.story = story;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public LocalDateTime getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(LocalDateTime generatedAt) {
		this.generatedAt = generatedAt;
	}
}
