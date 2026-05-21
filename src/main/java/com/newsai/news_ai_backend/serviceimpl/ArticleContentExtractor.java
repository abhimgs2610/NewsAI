package com.newsai.news_ai_backend.serviceimpl;

import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ArticleContentExtractor {

	private static final Logger logger = LoggerFactory.getLogger(ArticleContentExtractor.class);

	private static final int MAX_ARTICLE_CHARS = 10000;
	private static final int MIN_ARTICLE_CHARS = 500;

	public String extract(String url) {
		if (url == null || url.isBlank()) {
			return "";
		}

		try {
			Document document = Jsoup.connect(url)
					.userAgent("Mozilla/5.0 NewsAI/1.0")
					.timeout((int) Duration.ofSeconds(8).toMillis())
					.get();

			document.select("script, style, nav, footer, header, aside, form, noscript").remove();

			String articleText = document.select("article")
					.stream()
					.map(Element::text)
					.max(Comparator.comparingInt(String::length))
					.orElse("");

			if (articleText.length() < MIN_ARTICLE_CHARS) {
				articleText = document.select("p")
						.stream()
						.map(Element::text)
						.filter(text -> text.length() > 40)
						.collect(Collectors.joining(" "));
			}

			String extractedText = trim(clean(articleText), MAX_ARTICLE_CHARS);
			logger.info("Extracted {} chars from URL: {}", extractedText.length(), url);
			logger.info("Extracted article content: {}", extractedText);

			return extractedText;
		} catch (Exception e) {
			logger.warn("Could not extract article text from URL: {}", url, e);
			return "";
		}
	}

	private String clean(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("\\s+", " ").trim();
	}

	private String trim(String text, int maxLength) {
		if (text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength).trim();
	}
}
