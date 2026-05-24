package com.newsai.news_ai_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NewsAiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsAiBackendApplication.class, args);
	}

}
