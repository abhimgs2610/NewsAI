package com.newsai.news_ai_backend.dto;

import java.util.List;

public class NewsDiscoveryResponseDto {

	private String discoverRequestId;
	private String status;
	private String message;
	private String providerQuery;
	private List<NewsFeedDto> results;
	private boolean hasMore;
	private int readyCount;

	public NewsDiscoveryResponseDto() {
	}

	public NewsDiscoveryResponseDto(String status, String message, String providerQuery, List<NewsFeedDto> results) {
		this.status = status;
		this.message = message;
		this.providerQuery = providerQuery;
		this.results = results;
	}

	public String getDiscoverRequestId() {
		return discoverRequestId;
	}

	public void setDiscoverRequestId(String discoverRequestId) {
		this.discoverRequestId = discoverRequestId;
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

	public boolean isHasMore() {
		return hasMore;
	}

	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}

	public int getReadyCount() {
		return readyCount;
	}

	public void setReadyCount(int readyCount) {
		this.readyCount = readyCount;
	}
}