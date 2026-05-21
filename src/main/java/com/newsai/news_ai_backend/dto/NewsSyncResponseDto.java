package com.newsai.news_ai_backend.dto;

public class NewsSyncResponseDto {

	private int fetched;
	private int saved;
	private int updated;
	private int skipped;
	private int newsApiFetched;
	private int newsApiSaved;
	private int newsApiUpdated;
	private int newsApiSkipped;
	private int gNewsFetched;
	private int gNewsSaved;
	private int gNewsUpdated;
	private int gNewsSkipped;
	private int enriched;
	private int enrichmentSkipped;

	public NewsSyncResponseDto() {
	}

	public NewsSyncResponseDto(int fetched, int saved, int updated, int skipped) {
		this.fetched = fetched;
		this.saved = saved;
		this.updated = updated;
		this.skipped = skipped;
	}

	public int getFetched() {
		return fetched;
	}

	public void setFetched(int fetched) {
		this.fetched = fetched;
	}

	public int getSaved() {
		return saved;
	}

	public void setSaved(int saved) {
		this.saved = saved;
	}

	public int getUpdated() {
		return updated;
	}

	public void setUpdated(int updated) {
		this.updated = updated;
	}

	public int getSkipped() {
		return skipped;
	}

	public void setSkipped(int skipped) {
		this.skipped = skipped;
	}

	public int getNewsApiFetched() {
		return newsApiFetched;
	}

	public void setNewsApiFetched(int newsApiFetched) {
		this.newsApiFetched = newsApiFetched;
	}

	public int getNewsApiSaved() {
		return newsApiSaved;
	}

	public void setNewsApiSaved(int newsApiSaved) {
		this.newsApiSaved = newsApiSaved;
	}

	public int getNewsApiUpdated() {
		return newsApiUpdated;
	}

	public void setNewsApiUpdated(int newsApiUpdated) {
		this.newsApiUpdated = newsApiUpdated;
	}

	public int getNewsApiSkipped() {
		return newsApiSkipped;
	}

	public void setNewsApiSkipped(int newsApiSkipped) {
		this.newsApiSkipped = newsApiSkipped;
	}

	public int getGNewsFetched() {
		return gNewsFetched;
	}

	public void setGNewsFetched(int gNewsFetched) {
		this.gNewsFetched = gNewsFetched;
	}

	public int getGNewsSaved() {
		return gNewsSaved;
	}

	public void setGNewsSaved(int gNewsSaved) {
		this.gNewsSaved = gNewsSaved;
	}

	public int getGNewsUpdated() {
		return gNewsUpdated;
	}

	public void setGNewsUpdated(int gNewsUpdated) {
		this.gNewsUpdated = gNewsUpdated;
	}

	public int getGNewsSkipped() {
		return gNewsSkipped;
	}

	public void setGNewsSkipped(int gNewsSkipped) {
		this.gNewsSkipped = gNewsSkipped;
	}

	public int getEnriched() {
		return enriched;
	}

	public void setEnriched(int enriched) {
		this.enriched = enriched;
	}

	public int getEnrichmentSkipped() {
		return enrichmentSkipped;
	}

	public void setEnrichmentSkipped(int enrichmentSkipped) {
		this.enrichmentSkipped = enrichmentSkipped;
	}
}
