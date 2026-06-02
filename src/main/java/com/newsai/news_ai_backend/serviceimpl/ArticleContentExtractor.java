package com.newsai.news_ai_backend.serviceimpl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ArticleContentExtractor {

	private static final Logger logger = LoggerFactory.getLogger(ArticleContentExtractor.class);

	private static final int MAX_ARTICLE_CHARS = 10000;
	private static final int MIN_ARTICLE_CHARS = 500;
	private static final int LOG_PREVIEW_CHARS = 500;
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
			+ "(KHTML, like Gecko) Chrome/125.0 Safari/537.36";
	private static final String ARTICLE_SELECTORS = String.join(", ", List.of(
			"article",
			"main",
			"[itemprop=articleBody]",
			"[data-articlebody]",
			"[data-testid=article-body]",
			".articleBody",
			".article-body",
			".article-content",
			".article__content",
			".story-content",
			".story__content",
			".story_details",
			".story-body",
			".content__article-body",
			".entry-content",
			".post-content",
			".td-post-content",
			".Normal"));

	private final ObjectMapper objectMapper = new ObjectMapper();

	public String extract(String url) {
		if (url == null || url.isBlank()) {
			return "";
		}

		try {
			Document document = Jsoup.connect(url)
					.userAgent(USER_AGENT)
					.referrer("https://www.google.com/")
					.ignoreContentType(true)
					.followRedirects(true)
					.timeout((int) Duration.ofSeconds(12).toMillis())
					.get();

			String articleText = bestText(List.of(
					extractJsonLdArticleBody(document),
					extractFromArticleSelectors(document),
					extractParagraphText(document),
					extractMetaDescription(document)));

			String extractedText = trim(clean(articleText), MAX_ARTICLE_CHARS);
			logger.info("Extracted {} chars from URL: {}", extractedText.length(), url);
			if (!extractedText.isBlank()) {
				logger.info("Extracted article content preview: {}", trim(extractedText, LOG_PREVIEW_CHARS));
			}

			return extractedText;
		} catch (Exception e) {
			logger.warn("Could not extract article text from URL: {}", url, e);
			return "";
		}
	}

	private String extractJsonLdArticleBody(Document document) {
		List<String> bodies = new ArrayList<>();
		document.select("script[type=application/ld+json]").forEach(script -> {
			String json = script.data();
			if (json == null || json.isBlank()) {
				json = script.html();
			}
			addJsonLdArticleBodies(json, bodies);
		});
		return bodies.stream()
				.map(this::clean)
				.max(Comparator.comparingInt(String::length))
				.orElse("");
	}

	private void addJsonLdArticleBodies(String json, List<String> bodies) {
		if (json == null || json.isBlank()) {
			return;
		}
		try {
			JsonNode root = objectMapper.readTree(json);
			collectJsonField(root, "articleBody", bodies);
		} catch (Exception ignored) {
			// Some publishers emit malformed or multiple JSON-LD blocks. Other extractors will handle fallback content.
		}
	}

	private void collectJsonField(JsonNode node, String fieldName, List<String> values) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return;
		}
		if (node.isObject()) {
			JsonNode value = node.get(fieldName);
			if (value != null && value.isTextual() && !value.asText().isBlank()) {
				values.add(value.asText());
			}
			node.fields().forEachRemaining(field -> collectJsonField(field.getValue(), fieldName, values));
			return;
		}
		if (node.isArray()) {
			node.forEach(child -> collectJsonField(child, fieldName, values));
		}
	}

	private String extractFromArticleSelectors(Document document) {
		Document copy = document.clone();
		removeNoise(copy);
		return copy.select(ARTICLE_SELECTORS)
				.stream()
				.map(Element::text)
				.map(this::clean)
				.filter(text -> text.length() >= MIN_ARTICLE_CHARS)
				.max(Comparator.comparingInt(String::length))
				.orElse("");
	}

	private String extractParagraphText(Document document) {
		Document copy = document.clone();
		removeNoise(copy);
		return copy.select("p")
				.stream()
				.map(Element::text)
				.map(this::clean)
				.filter(text -> text.length() > 40)
				.filter(text -> !isBoilerplate(text))
				.collect(Collectors.joining(" "));
	}

	private String extractMetaDescription(Document document) {
		return bestText(List.of(
				document.select("meta[property=og:description]").attr("content"),
				document.select("meta[name=twitter:description]").attr("content"),
				document.select("meta[name=description]").attr("content")));
	}

	private String bestText(List<String> candidates) {
		return candidates.stream()
				.map(this::clean)
				.filter(text -> !text.isBlank())
				.max(Comparator.comparingInt(String::length))
				.orElse("");
	}

	private void removeNoise(Document document) {
		document.select("script, style, nav, footer, header, aside, form, noscript, iframe, svg, button, "
				+ ".ad, .ads, .advertisement, .social, .share, .related, .newsletter").remove();
	}

	private boolean isBoilerplate(String text) {
		String lower = text.toLowerCase();
		return lower.contains("privacy policy")
				|| lower.contains("subscribe to")
				|| lower.contains("sign in")
				|| lower.contains("read more")
				|| lower.contains("follow us")
				|| lower.contains("advertisement")
				|| lower.contains("cookies");
	}

	private String clean(String text) {
		if (text == null) {
			return "";
		}
		return text.replace('\u00a0', ' ')
				.replaceAll("\\s+", " ")
				.trim();
	}

	private String trim(String text, int maxLength) {
		if (text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength).trim();
	}
}
