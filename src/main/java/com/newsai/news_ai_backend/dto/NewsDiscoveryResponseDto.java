package com.newsai.news_ai_backend.dto;

import java.util.List;

public class NewsDiscoveryResponseDto {

	private String status;
	private String message;
	private String providerQuery;
	private List<NewsFeedDto> results;

	public NewsDiscoveryResponseDto() {
	}

	public NewsDiscoveryResponseDto(String status, String message, String providerQuery, List<NewsFeedDto> results) {
		this.status = status;
		this.message = message;
		this.providerQuery = providerQuery;
		this.results = results;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getProviderQuery() {
		return providerQuery;
	}

	public void setProviderQuery(String providerQuery) {
		this.providerQuery = providerQuery;
	}

	public List<NewsFeedDto> getResults() {
		return results;
	}

	public void setResults(List<NewsFeedDto> results) {
		this.results = results;
	}
}