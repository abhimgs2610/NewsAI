-- Clean rebuild SQL for the 3-table pipeline.
-- Use this in local MySQL before starting app with spring.jpa.hibernate.ddl-auto=validate.

DROP TABLE IF EXISTS news_story;
DROP TABLE IF EXISTS news_ai_enrichment;
DROP TABLE IF EXISTS news_article;

CREATE TABLE news_article (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	url VARCHAR(1000) NOT NULL UNIQUE,
	title VARCHAR(1000),
	description VARCHAR(1000),
	content VARCHAR(2000),
	source VARCHAR(255),
	provider VARCHAR(255),
	image_url VARCHAR(1000),
	published_at DATE
);

CREATE INDEX idx_news_article_published_at ON news_article (published_at);

CREATE TABLE news_ai_enrichment (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	news_article_id BIGINT NOT NULL UNIQUE,
	category VARCHAR(255),
	good_headline VARCHAR(1000),
	brief_story VARCHAR(2000),
	importance_score INT,
	country VARCHAR(255),
	state VARCHAR(255),
	city VARCHAR(255),
	processed_at DATETIME(6),
	CONSTRAINT fk_news_ai_enrichment_article
		FOREIGN KEY (news_article_id) REFERENCES news_article(id)
);

CREATE INDEX idx_news_ai_enrichment_category_processed
	ON news_ai_enrichment (category, processed_at);
CREATE INDEX idx_news_ai_enrichment_state_city_processed
	ON news_ai_enrichment (state, city, processed_at);
CREATE INDEX idx_news_ai_enrichment_importance_processed
	ON news_ai_enrichment (importance_score, processed_at);

CREATE TABLE news_story (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	news_article_id BIGINT NOT NULL,
	story LONGTEXT,
	style VARCHAR(255),
	language VARCHAR(32),
	generated_at DATETIME(6),
	CONSTRAINT fk_news_story_article
		FOREIGN KEY (news_article_id) REFERENCES news_article(id),
	CONSTRAINT uk_news_story_article_style_language
		UNIQUE (news_article_id, style, language)
);

CREATE INDEX idx_news_story_article_style_language
	ON news_story (news_article_id, style, language);
