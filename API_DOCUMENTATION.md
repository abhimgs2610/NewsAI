# NewsAI API Documentation

Base URL:

```text
http://localhost:8080
```

All current APIs return the same response wrapper:

```json
{
  "data": {},
  "count": 1,
  "responseCode": 200,
  "responseMessage": "Record fetched successfully",
  "error": false
}
```

For list APIs, `data` is an array and `count` is the number of returned records.

```json
{
  "data": [],
  "count": 0,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

For invalid requests:

```json
{
  "data": null,
  "count": 0,
  "responseCode": 400,
  "responseMessage": "Invalid request. Please check the request and try again.",
  "error": true
}
```

For internal/server errors:

```json
{
  "data": null,
  "count": 0,
  "responseCode": 500,
  "responseMessage": "An error encountered. Please contact support.",
  "error": true
}
```

## Shared News Card DTO

Most feed-style APIs return this article card shape:

```json
{
  "id": 4411,
  "headline": "K2 Medical Research Opens Boston Area Site in Foxboro, MA",
  "briefStory": "K2 Medical Research opened its newest clinical research site in Foxboro, MA, expanding access to clinical trials and innovative treatments.",
  "category": "Health",
  "source": "PRNewswire",
  "imageUrl": "https://mma.prnewswire.com/media/2992607/K2_Medical_Research_K2_East_Providence_Team.jpg?p=facebook",
  "country": "United States",
  "state": "Massachusetts",
  "city": "Foxboro",
  "publishedAt": "2026-06-02"
}
```

Location fields may be empty strings when no valid value is available.

## Query Parameter Rules

`limit`:
- Must be an integer.
- Must not be negative.
- `limit=0` returns `200 OK` with empty data.
- Invalid values like `%`, `1%`, `1%1`, `#`, `1#`, `1&5`, or non-numeric values should return a `400` wrapper.

`q`:
- Optional search keyword.
- Can combine with country, state, city, and category filters.

Filter behavior:
- `country`, `state`, `city`, and `category` support matching existing enriched news.
- Combined filters work together.
- `country=World` means world feed, including non-India news and fallback World rows.

## 1. Fetch All News Feed

```http
GET /api/news/feed?limit=5
```

Example:

```text
http://localhost:8080/api/news/feed?limit=5
```

Response:

```json
{
  "data": [
    {
      "id": 4411,
      "headline": "K2 Medical Research Opens Boston Area Site in Foxboro, MA",
      "briefStory": "K2 Medical Research opened its newest clinical research site in Foxboro, MA, expanding access to clinical trials and innovative treatments.",
      "category": "Health",
      "source": "PRNewswire",
      "imageUrl": "https://mma.prnewswire.com/media/2992607/K2_Medical_Research_K2_East_Providence_Team.jpg?p=facebook",
      "country": "United States",
      "state": "Massachusetts",
      "city": "Foxboro",
      "publishedAt": "2026-06-02"
    }
  ],
  "count": 5,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

## 2. Fetch News by Country

```http
GET /api/news/feed?country=INDIA&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?country=INDIA&limit=2
```

Notes:
- Country matching is case-insensitive from the user's perspective.
- Use `country=World` for world news, meaning non-India news plus fallback World rows.

## 3. Fetch News by State

```http
GET /api/news/feed?state=P&limit=3
```

Example:

```text
http://localhost:8080/api/news/feed?state=P&limit=3
```

Notes:
- State filter can match partial values.
- Example `P` can match states such as `Punjab`, `Uttar Pradesh`, or `Saint Petersburg`.

## 4. Fetch News by City

```http
GET /api/news/feed?city=Chandigarh&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?city=Chandigarh&limit=2
```

## 5. Fetch News by Category

```http
GET /api/news/feed?category=Entertainment&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?category=Entertainment&limit=2
```

## 6. Fetch News by Combined Filters

```http
GET /api/news/feed?category=Sports&state=Punjab&city=Chandigarh&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?category=Sports&state=Punjab&city=Chandigarh&limit=2
```

Notes:
- Filters are combined.
- Returned articles must satisfy all supplied filters.

## 7. Fetch Hot News

```http
GET /api/news/hot?limit=2
```

Example:

```text
http://localhost:8080/api/news/hot?limit=2
```

Notes:
- Hot news is ordered by AI importance score and recency.
- Supports `q` search.

## 8. Get List of Countries

```http
GET /api/news/countries
```

Example:

```text
http://localhost:8080/api/news/countries
```

Response:

```json
{
  "data": [
    {
      "country": "Afghanistan",
      "newsCount": 1
    },
    {
      "country": "India",
      "newsCount": 190
    }
  ],
  "count": 6,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

Notes:
- Each country includes the available enriched news count.

## 9. Get List of States

```http
GET /api/news/states
```

Example:

```text
http://localhost:8080/api/news/states
```

Response:

```json
{
  "data": [
    "Andaman and Nicobar Islands",
    "Andhra Pradesh",
    "Bihar",
    "Chandigarh",
    "Delhi",
    "Gujarat"
  ],
  "count": 24,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

Notes:
- Returns India-only states.
- Blank, null, and unknown values are excluded.

## 10. Get List of Cities

```http
GET /api/news/cities
```

Example:

```text
http://localhost:8080/api/news/cities
```

Response:

```json
{
  "data": [
    "Ahmedabad",
    "Bengaluru",
    "Bhopal",
    "Chandigarh",
    "New Delhi"
  ],
  "count": 36,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

Notes:
- Returns India-only cities.
- Blank, null, and unknown values are excluded.

## 11. Get List of Cities by State

```http
GET /api/news/cities?state=Gujarat
```

Example:

```text
http://localhost:8080/api/news/cities?state=Gujarat
```

Response:

```json
{
  "data": [
    "Ahmedabad",
    "Surat",
    "Vadodara"
  ],
  "count": 3,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

## 12. Get List of Categories

```http
GET /api/news/categories
```

Example:

```text
http://localhost:8080/api/news/categories
```

Response:

```json
{
  "data": [
    "Arts",
    "Business",
    "Crime",
    "Education",
    "Entertainment",
    "Politics",
    "Sports",
    "Technology"
  ],
  "count": 38,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

Notes:
- Returns India-only categories.
- Blank, null, and unknown values are excluded.

## 13. Search in All News Feed

```http
GET /api/news/feed?q=trump&limit=3
```

Example:

```text
http://localhost:8080/api/news/feed?q=trump&limit=3
```

Notes:
- Searches across feed content.
- Can be combined with filters.

## 14. Search News in Country

```http
GET /api/news/feed?country=india&q=US&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?country=india&q=US&limit=2
```

## 15. Search News in State

```http
GET /api/news/feed?state=Delhi&q=cen&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?state=Delhi&q=cen&limit=2
```

## 16. Search News in City

```http
GET /api/news/feed?city=New Delhi&q=&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?city=New%20Delhi&q=&limit=2
```

Notes:
- Empty `q` is allowed and behaves like no search keyword.

## 17. Search News in Category

```http
GET /api/news/feed?category=Politics&q=inter&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?category=Politics&q=inter&limit=2
```

## 18. Search News in Combined Filters

```http
GET /api/news/feed?category=Sports&state=Gujarat&city=Ahmedabad&q=IPL&limit=2
```

Example:

```text
http://localhost:8080/api/news/feed?category=Sports&state=Gujarat&city=Ahmedabad&q=IPL&limit=2
```

## 19. Search in Hot News

```http
GET /api/news/hot?q=trump&limit=2
```

Example:

```text
http://localhost:8080/api/news/hot?q=trump&limit=2
```

Notes:
- Search is applied on hot news, while ordering remains based on importance score.

## 20. Fetch Story

```http
GET /api/news/{id}?language=ENGLISH&style=genz&refresh=true
```

Example:

```text
http://localhost:8080/api/news/3528?language=ENGLISH&style=genz&refresh=true
```

Query parameters:

| Name | Required | Example | Notes |
| --- | --- | --- | --- |
| `language` | No | `ENGLISH` | Supported values include `ENGLISH` and `HINDI`. |
| `style` | No | `genz` | Story style. Current main style is `genz`. |
| `refresh` | No | `true` | If `true`, regenerates story using Groq. If `false`, cached story can be returned. |

Response:

```json
{
  "data": {
    "story": "You know the King of Pop, right? Michael Jackson..."
  },
  "count": 1,
  "responseCode": 200,
  "responseMessage": "Record fetched successfully",
  "error": false
}
```

Notes:
- Uses extracted article content when available.
- Saves generated story in `news_story`.
- Reuses cached story unless refresh is requested.

## 21. Ask Follow-Up Question on News

```http
POST /api/news/{id}/ask
```

Example:

```text
http://localhost:8080/api/news/3528/ask
```

Payload:

```json
{
  "question": "when its going to release on Netflix?",
  "language": "ENGLISH"
}
```

Response:

```json
{
  "data": {
    "answer": "You're asking when the Netflix documentary series about Michael Jackson's trial will be available on the platform..."
  },
  "count": 1,
  "responseCode": 200,
  "responseMessage": "Record fetched successfully",
  "error": false
}
```

Notes:
- Sends article title, raw article data, extracted/fallback article context, generated story, user question, and previous Q&A history to Groq.
- Saves each question and answer in `news_chat_message`.
- Follow-up questions like `then why?`, `after that?`, or `what date?` can use previous chat history.

## 22. Discover News

```http
POST /api/news/discover
```

Example:

```text
http://localhost:8080/api/news/discover
```

Payload:

```json
{
  "context": "History of Ramayana",
  "country": "",
  "state": "",
  "city": "",
  "loadMore": false
}
```

Response:

```json
{
  "data": {
    "discoverRequestId": "DISC-example-id",
    "status": "PROCESSING",
    "message": "Records fetched successfully",
    "providerQuery": "History of Ramayana",
    "results": [
      {
        "id": 4569,
        "headline": "Ramayana: Ranbir Kapoor's magnum opus creates history",
        "briefStory": "Ramayana is already being hailed as the grandest cinematic spectacle of 2026.",
        "category": "Entertainment",
        "source": "Firstpost",
        "imageUrl": "https://images.firstpost.com/example.jpg",
        "country": "India",
        "state": "",
        "city": "",
        "publishedAt": "2026-05-19"
      }
    ],
    "hasMore": true,
    "readyCount": 1
  },
  "count": 1,
  "responseCode": 200,
  "responseMessage": "Records fetched successfully",
  "error": false
}
```

Notes:
- Used for "Could not find the news?" flow.
- `context` is mandatory for the first request.
- `country`, `state`, and `city` are optional.
- If matching news exists locally, it can return local results immediately.
- Provider fetch and extra enrichment run in the background after the first response.

## Frontend Usage Flow

Suggested app flow:

1. Load country list from `GET /api/news/countries`.
2. Load India filter lists from:
   - `GET /api/news/states`
   - `GET /api/news/cities`
   - `GET /api/news/categories`
3. Show main feed from `GET /api/news/feed?limit=20`.
4. Apply filters by adding `country`, `state`, `city`, `category`, and `q`.
5. Show hot news from `GET /api/news/hot?limit=20`.
6. Open detail/story page with `GET /api/news/{id}?language=ENGLISH&style=genz&refresh=false`.
7. Use story chat with `POST /api/news/{id}/ask`.
8. Use discover flow with `POST /api/news/discover` when the user cannot find a news item.


Updated discover behavior:
- Do not pass `limit`; backend returns max 5 ready results per call.
- First request creates `discoverRequestId`, starts background provider fetch/enrichment, waits up to 5 seconds for initial ready results, then returns max 5.
- Use the same endpoint for load more with `loadMore=true` and `discoverRequestId`.
- Load more returns max 5 ready unsent results immediately. If only 1, 2, 3, or 4 are ready, it returns those.
- If no result is ready yet and background work is still running, status is `PROCESSING`, results are empty, and `hasMore=true`.

Load more payload:

```json
{
  "discoverRequestId": "DISC-example-id",
  "loadMore": true
}
```