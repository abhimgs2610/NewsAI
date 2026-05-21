package com.newsai.news_ai_backend.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.newsai.news_ai_backend.model.StoryLanguage;

class NewsServiceImplTest {

	@Test
	void storyLanguageDefaultsToEnglishWhenBlank() {
		assertEquals(StoryLanguage.ENGLISH, StoryLanguage.fromInput(""));
	}

	@Test
	void storyLanguageAcceptsHindiCaseInsensitive() {
		assertEquals(StoryLanguage.HINDI, StoryLanguage.fromInput("hindi"));
	}

	@Test
	void storyLanguageRejectsUnsupportedLanguage() {
		assertThrows(IllegalArgumentException.class, () -> StoryLanguage.fromInput("HINGLISH"));
	}
}
