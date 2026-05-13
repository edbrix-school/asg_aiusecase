# ags-ai-usecase

Standalone Spring Boot microservice for AI-powered inventory, stock, and unit semantic matching.

This service does not call or depend on the old `ai-usecase` or `als-shipchandling` runtimes. It owns its PostgreSQL schema, pgvector embeddings, Redis caches, RabbitMQ embedding pipeline, business rules, and LLM ambiguity resolution.

## Stack

- Java 21
- Spring Boot 3.5
- Spring AI 1.1.6
- OpenAI Java SDK dependency 4.35.0
- PostgreSQL + pgvector for vector side tables, caches, and AI-owned schema
- Oracle common database for current `STOCK_MASTER` and `STOCK_UNIT_MASTER` reads; switch `COMMON_DB_*` settings when this moves to PostgreSQL
- Redis
- RabbitMQ
- Optional OpenTelemetry, Prometheus, Grafana, Loki, Jaeger
- Docker Compose
- Maven

## Run

Create an `.env` file:

```bash
cp .env.example .env
```

Optional local build:

```bash
mvn clean package
```

Run core services only:

```bash
docker compose up --build
```

Run with optional observability:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

Core service URL:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

RabbitMQ:

```text
http://localhost:15672
```

Grafana, only with observability:

```text
http://localhost:3000
```

## API

Required endpoints:

```text
POST /search
POST /stock/embedding-events
GET /stock/common-db
GET /stock-unit/common-db
DELETE /stock/embeddings/local
POST /stock/embeddings/sync-all
```

`POST /stock/embedding-events` is the integration endpoint other microservices call after they complete CRUD in the common database. This service publishes the event to RabbitMQ; the embedding worker then reloads the latest row from `STOCK_MASTER` or `STOCK_UNIT_MASTER` through the configured common DB connection and updates the PostgreSQL vector side table.

`GET /stock/common-db` and `GET /stock-unit/common-db` return the current active, non-deleted records from common DB.

`DELETE /stock/embeddings/local` clears local vector embedding tables.

`POST /stock/embeddings/sync-all` rebuilds local stock and stock-unit embeddings from all active, non-deleted rows in common DB.

## Databases

- `spring.datasource.*` points to this service's PostgreSQL database. Flyway, Redis-backed cache entities, LLM cache entities, and pgvector side tables live here.
- `app.common-db.datasource.*` points to the shared stock database. It is Oracle today and can be changed to PostgreSQL later by changing `COMMON_DB_URL`, `COMMON_DB_USERNAME`, `COMMON_DB_PASSWORD`, and `COMMON_DB_DRIVER`.
- Search reads only the vector side tables in PostgreSQL. Embedding events refresh those side tables from the common DB, so cross-database joins are not required.

## Matching Safety

The resolver follows this order:

1. Redis final response cache
2. Redis semantic cache
3. Query embedding
4. Redis vector result cache
5. Parallel pgvector stock and stock-unit search
6. Stock code/stock name/synonym checks
7. Stock-unit matching
8. Confidence scoring
9. Optional batched LLM ambiguity resolution
10. Final hard validation

LLM output is never trusted directly. It can only select from retrieved candidate `stockPoid` and `stockUnitPoid` pairs. If it returns anything outside those candidates, the service rejects it and falls back to the best validated vector result.

## Sample Data

Sample seed data lives at:

```text
src/main/resources/db/seed/sample-data.sql
```

Load it manually after startup:

```bash
psql "$DB_URL" -f src/main/resources/db/seed/sample-data.sql
```

After loading seed data, publish update events or call update APIs to generate embeddings. Create/update APIs automatically enqueue embedding generation.

## Configuration

Important environment variables:

```text
OPENAI_API_KEY
AI_EMBEDDING_MODEL
AI_CHAT_MODEL
AI_FALLBACK_MODEL
AI_REASONING_EFFORT
MATCH_TOP_K
MATCH_SIMILARITY_THRESHOLD
MATCH_HIGH_CONFIDENCE_THRESHOLD
MATCH_AMBIGUITY_DELTA
SERVICE_API_KEY
```

API-key protection is disabled by default. To enable it:

```text
SERVICE_AUTH_ENABLED=true
SERVICE_API_KEY=your-service-key
```

Per-request model override is supported on `/search`:

```json
{
  "query": "french orange crate",
  "embeddingModel": "text-embedding-3-small",
  "chatModel": "gpt-5.5",
  "fallbackModel": "gpt-5.4-mini"
}
```

## Observability

Observability is off by default.

Default profile:

```text
management.tracing.enabled=false
management.prometheus.metrics.export.enabled=false
app.observability.enabled=false
```

Enable it with:

```bash
SPRING_PROFILES_ACTIVE=observability
```

or use:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

The optional stack includes:

- Prometheus metrics
- Grafana dashboard provisioning
- Loki log collection
- Jaeger tracing
- OpenTelemetry collector

## Design Docs

- [Architecture](docs/architecture.md)
- [Sample API requests](docs/sample-requests.http)
