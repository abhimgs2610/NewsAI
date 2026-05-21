package com.newsai.news_ai_backend.serviceimpl;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.newsai.news_ai_backend.client.OpenAiClient;
import com.newsai.news_ai_backend.service.StoryGenerator;

@Service
@Profile("!mock")
public class OpenAiStoryGenerator implements StoryGenerator {

	private final OpenAiClient openAiClient;

	public OpenAiStoryGenerator(OpenAiClient openAiClient) {
		this.openAiClient = openAiClient;
	}

	@Override
	public String buildStory(String title, String context, String category, String style, String language) {
		return openAiClient.generateStory(buildPrompt(title, context, style, category, language));
	}

	private String buildPrompt(String title, String sourceText, String style, String category, String language) {
		String requestedStyle = style == null || style.isBlank() ? "genz" : style.trim();
		String requestedLanguage = language == null || language.isBlank() ? "ENGLISH" : language.trim();

		return "You are NewsAI: a personality-led news storyteller for audio.\n"
				+ "Do not sound like a newspaper summary. Tell the news like a smart, funny podcaster or standup-style explainer "
				+ "who knows the facts and knows how people talk online.\n\n"
				+ "Make the story entertaining, sarcastic where it fits, culturally relatable, and easy to remember. "
				+ "Slang, Hinglish, punchlines, and side-comments are allowed when they make the story connect better. "
				+ "Use them naturally, not randomly. Do not overuse the same slang, opener, transition, or catchphrase again and again.\n\n"
				+ "Narrate it like a real story people can map in their head: first explain the backstory or past situation that led here, "
				+ "then show how that past is impacting the present news, then reveal what happened now and why it matters. "
				+ "The listener should feel: 'Oh, this happened earlier, that's why today's news matters.'\n\n"
				+ "Start with a strong hook, then build the backstory, connect it to the current event, explain the impact, "
				+ "and end with a memorable line. Make it as long as needed to feel complete, but avoid filler and repetition.\n\n"
				+ "Stay factual: use only details from the source, keep allegations/speculation as allegations/speculation, "
				+ "do not invent numbers, backstory, causes, or outcomes, and do not mix facts between people. "
				+ "If background context is provided, use it to explain the past situation behind the current news, "
				+ "but keep the main article as the primary source. If context conflicts or feels unrelated, ignore it. "
				+ "If there is still not enough backstory, keep it honest instead of guessing. "
				+ "If the topic is tragic, violent, health-related, or deeply sensitive, keep the personality but drop cheap jokes.\n\n"
				+ "Return only the final story. No markdown, bullets, headings, or stage directions.\n\n"
				+ "Generate story language strictly in: " + requestedLanguage + "\n"
				+ "Requested style: " + requestedStyle + "\n"
				+ "Category: " + safe(category) + "\n"
				+ "Title: " + safe(title) + "\n"
				+ "Source text:\n" + safe(sourceText);
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
