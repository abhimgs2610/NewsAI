package com.newsai.news_ai_backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.newsai.news_ai_backend.dto.NewsSyncResponseDto;
import com.newsai.news_ai_backend.serviceimpl.NewsSyncService;

@RestController
@RequestMapping("/api/news")
public class NewsSyncController {

	private final NewsSyncService newsSyncService;

	public NewsSyncController(NewsSyncService newsSyncService) {
		this.newsSyncService = newsSyncService;
	}

	@PostMapping("/sync")
	public NewsSyncResponseDto syncNews(@RequestParam(value = "hours", defaultValue = "24") int hours) {
		return newsSyncService.syncLatestIndiaNews(hours);
	}
}
