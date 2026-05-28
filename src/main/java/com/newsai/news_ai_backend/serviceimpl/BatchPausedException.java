package com.newsai.news_ai_backend.serviceimpl;

public class BatchPausedException extends RuntimeException {

	public BatchPausedException(String message) {
		super(message);
	}
}