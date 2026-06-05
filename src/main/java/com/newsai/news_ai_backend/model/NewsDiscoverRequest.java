package com.newsai.news_ai_backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "news_discover_request", indexes = {
		@Index(name = "idx_news_discover_request_key", columnList = "request_key"),
		@Index(name = "idx_news_discover_request_status", columnList = "status,created_at")
})
public class NewsDiscoverRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "request_key", nullable = false, unique = true, length = 64)
	private String requestKey;

	@Column(name = "context", length = 1000)
	private String context;

	@Column(name = "country", length = 255)
	private String country;

	@Column(name = "state", length = 255)
	private String state;

	@Column(name = "city", length = 255)
	private String city;

	@Column(name = "provider_query", length = 1500)
	private String providerQuery;

	@Column(name = "status", length = 32)
	private String status;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRequestKey() {
		return requestKey;
	}

	public void setRequestKey(String requestKey) {
		this.requestKey = requestKey;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
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

	public String getProviderQuery() {
		return providerQuery;
	}

	public void setProviderQuery(String providerQuery) {
		this.providerQuery = providerQuery;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}