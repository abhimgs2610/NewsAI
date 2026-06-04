package com.newsai.news_ai_backend.dto;

public class NewsAskResponseDto {

	private String answer;

	public NewsAskResponseDto() {
	}

	public NewsAskResponseDto(String answer) {
		this.answer = answer;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}
}
