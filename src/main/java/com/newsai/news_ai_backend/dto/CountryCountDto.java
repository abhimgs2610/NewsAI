package com.newsai.news_ai_backend.dto;

public class CountryCountDto {

	private String country;
	private long newsCount;

	public CountryCountDto() {
	}

	public CountryCountDto(String country, long newsCount) {
		this.country = country;
		this.newsCount = newsCount;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public long getNewsCount() {
		return newsCount;
	}

	public void setNewsCount(long newsCount) {
		this.newsCount = newsCount;
	}
}
