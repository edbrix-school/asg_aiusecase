# ags-ai-usecase

Standalone Spring Boot microservice for AI-powered inventory, stock, and unit semantic matching.

This service does not call or depend on the old `ai-usecase` or `als-shipchandling` runtimes. It owns its PostgreSQL schema, pgvector embeddings, Redis caches, RabbitMQ embedding pipeline, business rules, and LLM ambiguity resolution.

## Stack

- Java 21
- Spring Boot 3.5
- Spring AI 1.1.6
- OpenAI Java SDK dependency 4.35.0
- PostgreSQL + pgvector
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
POST /inventory
POST /unit
PUT /inventory/{id}
PUT /unit/{id}
```

Extra read endpoints are included for operations:

```text
GET /inventory
GET /inventory/{id}
GET /unit?inventoryId={id}
GET /unit/{id}
```

## Matching Safety

The resolver follows this order:

1. Redis final response cache
2. Redis semantic cache
3. Query embedding
4. Redis vector result cache
5. Parallel pgvector inventory and unit search
6. Stock code/product code/synonym checks
7. Unit filtering
8. Compatibility rule validation
9. Confidence scoring
10. Optional batched LLM ambiguity resolution
11. Final hard validation

LLM output is never trusted directly. It can only select from business-approved candidate `inventoryId` and `unitId` pairs. If it returns anything outside those candidates, the service rejects it and falls back to the best validated business-rule result.

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
