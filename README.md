# NewsAI

NewsAI is a Spring Boot backend for a modern news storytelling platform. It does more than fetch headlines: it collects real news from external providers, enriches each article with AI, and turns selected articles into human-sounding stories designed for audio-style consumption.

The goal is simple: explain what happened, explain why it matters, and tell it in a way people actually want to read or listen to.

## Why This Project Exists

Most news apps show short robotic summaries, category tags, and endless lists of articles. NewsAI is built around a different idea: people connect better with context, backstory, tone, and narrative.

NewsAI fetches real news, stores raw provider data, enriches it with AI, and generates stories in a conversational podcast-like style while keeping the system fact-grounded.

## Core Features

- Fetch latest India and world news from NewsAPI and GNews.
- Store raw provider articles separately from AI-generated data.
- Enrich articles using Groq with category, headline, brief story, importance score, country, state, and city.
- Browse feed with search and filters for country, state, city, and category. Feed defaults to India news, while `searchValue=true` searches globally.
- Support `country=World` as a world feed for non-India news and fallback World rows.
- Fetch hot news ordered by AI importance score and recency.
- Generate long-form NewsAI stories in a human, conversational style.
- Cache generated stories to avoid unnecessary AI calls.
- Ask follow-up questions on a news story using chat history.
- Discover missing news when the user cannot find what they are looking for.
- Process discover results asynchronously with load-more support.
- Support frontend global search, view-all pagination, story reader, story Q&A, and saved-story UI flows.
- Run scheduled batch sync automatically.
- Pause batch processing using a configured `STOP.txt` file.
- Support offset pagination on feed and hot news for UI load-more flows.
- Return all APIs in a consistent response wrapper.
- Log internal errors without exposing sensitive details in API responses.

## Tech Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL
- Groq Chat Completions API
- NewsAPI
- GNews
- Jsoup for article content extraction
- Maven

## Architecture Overview

NewsAI uses a pipeline-based backend design instead of putting everything in one table.

```text
NewsAPI / GNews
      |
      v
news_article              raw provider data
      |
      v
Groq enrichment
      |
      v
news_ai_enrichment        product feed data
      |
      +--> feed / hot / filters
      |
      v
Groq story generation
      |
      v
news_story                cached long-form stories
      |
      v
news_chat_message         follow-up Q&A history
```

Discover flow uses separate session tables:

```text
POST /api/news/discover
      |
      v
news_discover_request     one discover session
      |
      v
background provider fetch + enrichment
      |
      v
news_discover_result      ready results + sent/not-sent tracking
      |
      v
loadMore returns next unsent results
```

## Database Tables

### `news_article`
Stores raw data from NewsAPI and GNews.

Important columns:
- `url`
- `title`
- `description`
- `content`
- `source`
- `provider`
- `image_url`
- `published_at`
- `extracted_content`
- `extracted_at`

### `news_ai_enrichment`
Stores AI-enriched data used by the main product feed.

Important columns:
- `news_article_id`
- `category`
- `good_headline`
- `brief_story`
- `importance_score`
- `country`
- `state`
- `city`
- `processed_at`

### `news_story`
Stores cached generated stories for each article, language, and style.

Important columns:
- `news_article_id`
- `story`
- `style`
- `language`
- `generated_at`

### `news_chat_message`
Stores follow-up questions and answers for a particular news article.

Important columns:
- `news_article_id`
- `question`
- `answer`
- `asked_at`

### `news_discover_request`
Stores one discover session created when a user searches for missing news.

Important columns:
- `request_key`
- `context`
- `country`
- `state`
- `city`
- `provider_query`
- `status`
- `created_at`
- `updated_at`

### `news_discover_result`
Stores discover results and tracks whether each result has already been sent to the user.

Important columns:
- `discover_request_id`
- `news_article_id`
- `sent_to_user`
- `display_order`
- `created_at`

## Main API Areas

Full API details are documented in [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

Main API groups:

- `GET /api/news/feed` - India-default feed, filters, search, and offset pagination.
- `GET /api/news/hot` - hot news by importance score with offset pagination.
- `GET /api/news/countries` - country list with news counts.
- `GET /api/news/states` - India-only state list.
- `GET /api/news/cities` - India-only city list, optionally by state.
- `GET /api/news/categories` - India-only category list.
- `GET /api/news/{id}` - generate or fetch cached NewsAI story.
- `POST /api/news/{id}/ask` - ask follow-up questions on a story.
- `POST /api/news/discover` - discover news when the user cannot find it.
- `POST /api/news/sync` - fetch and enrich latest news.

## Discover Flow

The discover API is designed for the UI action: "Could not find the news?"

First request:

```http
POST /api/news/discover
```

```json
{
  "context": "Pune Wipro case",
  "country": "",
  "state": "",
  "city": "",
  "loadMore": false
}
```

Behavior:
- Creates a `discoverRequestId`.
- Starts provider fetch and AI enrichment in the background.
- Waits up to 5 seconds for initial ready results.
- Returns max 5 ready results.
- Background processing continues after the response.

Load more request:

```json
{
  "discoverRequestId": "DISC-example-id",
  "loadMore": true
}
```

Behavior:
- Returns max 5 unsent ready results.
- If only 1, 2, 3, or 4 are ready, it returns those immediately.
- If none are ready but processing is still running, returns empty results with `status=PROCESSING` and `hasMore=true`.
- When everything is sent and processing is complete, returns `hasMore=false`.

## AI Behavior

NewsAI uses Groq for three main AI tasks:

1. Article enrichment
   - category
   - improved headline
   - brief story
   - importance score
   - country/state/city

2. Story generation
   - backstory first
   - current event after that
   - human, conversational, audio-friendly style
   - engaging tone without turning into unsupported promotion or fake facts

3. Follow-up Q&A
   - uses article fields
   - extracted or fallback article content
   - generated story
   - previous Q&A history for that article

## Batch Processing

NewsAI can run a scheduled sync job that fetches India and world news from providers and enriches available articles.

Configured properties:

```properties
sync.scheduler.enabled=true
sync.scheduler.hours=24
sync.scheduler.initialDelayMs=60000
sync.scheduler.fixedDelayMs=3600000
```

The batch can be paused without stopping the application by placing a configured stop file.

```properties
sync.stop-file.enabled=true
sync.stop-file.directory=C:/users2/newsAIBatch
sync.stop-file.name=STOP.txt
sync.stop-file.pauseDurationMs=1800000
```

When `STOP.txt` is detected, batch processing pauses for the configured duration.

## Setup

### 1. Create MySQL Database

```sql
CREATE DATABASE newsdb;
```

Use `src/main/resources/db-notes.sql` for the current table setup.

### 2. Configure Environment Variables

The application reads API keys from environment variables:

```text
NEWSAPI_API_KEY
GNEWS_API_KEY
OPENAI_API_KEY
```

`OPENAI_API_KEY` is used for the Groq API key because the Groq endpoint follows the OpenAI-compatible chat completions format.

### 3. Configure Database

Default local configuration in `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/newsdb
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=validate
```

Update these values for your local MySQL setup if needed.

### 4. Build

```powershell
.\mvnw.cmd -q -DskipTests compile
```

### 5. Test

```powershell
.\mvnw.cmd test
```

### 6. Run

Start the Spring Boot application from Eclipse or run:

```powershell
.\mvnw.cmd spring-boot:run
```

Default server:

```text
http://localhost:8080
```

## Example API Calls

Fetch feed. Country defaults to India when omitted:

```http
GET http://localhost:8080/api/news/feed?limit=5
```

Search India-default feed:

```http
GET http://localhost:8080/api/news/feed?q=trump&limit=3
```

Search all news globally from the UI search box:

```http
GET http://localhost:8080/api/news/feed?q=Michael%20Jackson&searchValue=true&limit=20
```

Fetch India news:

```http
GET http://localhost:8080/api/news/feed?country=India&limit=5
```

Fetch world news:

```http
GET http://localhost:8080/api/news/feed?country=World&limit=5
```

Load more feed records:

```http
GET http://localhost:8080/api/news/feed?country=India&limit=50&offset=50
```

Fetch hot news:

```http
GET http://localhost:8080/api/news/hot?limit=20&offset=0
```

Load more hot news:

```http
GET http://localhost:8080/api/news/hot?limit=50&offset=50
```

Fetch story:

```http
GET http://localhost:8080/api/news/3528?language=ENGLISH&style=genz&refresh=false
```

Ask follow-up question:

```http
POST http://localhost:8080/api/news/3528/ask
```

```json
{
  "question": "when is it going to release on Netflix?",
  "language": "ENGLISH"
}
```

Discover missing news:

```http
POST http://localhost:8080/api/news/discover
```

```json
{
  "context": "James Handy death",
  "country": "",
  "state": "",
  "city": "",
  "loadMore": false
}
```

## Response Format

All APIs use a common response wrapper:

```json
{
  "data": {},
  "count": 1,
  "responseCode": 200,
  "responseMessage": "Record fetched successfully",
  "error": false
}
```

Errors return a generic message to avoid exposing sensitive internal details:

```json
{
  "data": null,
  "count": 0,
  "responseCode": 500,
  "responseMessage": "An error encountered. Please contact support.",
  "error": true
}
```

Internal exception details are written to logs.

## Current Status

Backend v1 is feature-complete for the current product plan:

- News ingestion is implemented.
- AI enrichment is implemented.
- Feed/search/filter APIs are implemented.
- Hot news API is implemented.
- Story generation and caching are implemented.
- Follow-up Q&A chat is implemented.
- Async discover with load more is implemented.
- Batch scheduling and pause control are implemented.
- API documentation is available.
- Angular frontend integration is underway with Home, Explore, Discover, Story Reader, Story Chat, and Saved Stories flows.

Next major step: continue polishing the frontend experience and add production-ready UI features such as account-backed saved stories, history, profile, and settings.


