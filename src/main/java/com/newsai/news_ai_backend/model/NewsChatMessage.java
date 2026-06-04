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

@Entity
@Table(name = "news_chat_message", indexes = {
		@Index(name = "idx_news_chat_article_asked", columnList = "news_article_id,asked_at")
})
public class NewsChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "news_article_id", nullable = false)
	private NewsArticle newsArticle;

	@Column(name = "question", columnDefinition = "LONGTEXT")
	private String question;

	@Column(name = "answer", columnDefinition = "LONGTEXT")
	private String answer;

	@Column(name = "asked_at")
	private LocalDateTime askedAt;

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

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public LocalDateTime getAskedAt() {
		return askedAt;
	}

	public void setAskedAt(LocalDateTime askedAt) {
		this.askedAt = askedAt;
	}
}
