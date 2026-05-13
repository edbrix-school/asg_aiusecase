# Architecture

## Component View

```mermaid
flowchart LR
    Client["Client"] --> API["Spring Boot API"]
    API --> Cache["Redis caches"]
    API --> Rules["Business Rule Engine"]
    API --> Embed["Embedding Service"]
    Embed --> OpenAI["OpenAI Embeddings"]
    Rules --> Vector["pgvector Search"]
    Vector --> Postgres["PostgreSQL vector side tables"]
    Rules --> LLM["LLM Ambiguity Resolver"]
    LLM --> OpenAIResponses["OpenAI Responses API"]
    API --> Rabbit["RabbitMQ Events"]
    Rabbit --> Worker["Embedding Worker"]
    Worker --> Embed
    Worker --> Postgres
    Worker --> Cache
```

## Search Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Search API
    participant R as Redis
    participant E as Embedding Service
    participant V as pgvector
    participant B as Business Rules
    participant L as LLM Resolver

    C->>API: POST /search
    API->>R: final cache lookup
    alt final cache hit
        R-->>API: cached response
        API-->>C: reasoningSource=CACHE
    else cache miss
        API->>R: semantic cache lookup
        API->>E: generate query embedding
        API->>R: vector cache lookup
        alt vector cache miss
            par stock search
                API->>V: stock cosine search
            and stock-unit search
                API->>V: stock-unit cosine search
            end
        end
        API->>B: exact, synonym, stock-unit, confidence
        alt high confidence
            B-->>API: selected validated match
            API-->>C: reasoningSource=VECTOR
        else ambiguous
            API->>L: one batched LLM request with allowed candidates
            L-->>API: selected stockPoid/stockUnitPoid
            API->>B: validate selected IDs are in the retrieved candidate set
            API-->>C: reasoningSource=LLM or VECTOR fallback
        end
    end
```

## Async Embedding Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Stock Event API
    participant DB as Common Oracle/PostgreSQL stock DB
    participant VDB as PostgreSQL vector side tables
    participant MQ as RabbitMQ
    participant W as Embedding Worker
    participant AI as OpenAI Embeddings
    participant R as Redis

    C->>API: POST /stock/embedding-events after external CRUD
    API->>MQ: publish durable event
    API-->>C: 202 Accepted
    MQ->>W: consume event
    W->>DB: load latest STOCK_MASTER/STOCK_UNIT_MASTER row
    W->>AI: generate embedding from compact JSON
    W->>VDB: upsert stock_vector_embedding or stock_unit_vector_embedding
    W->>R: increment cache version
```

## Hallucination Boundary

The LLM receives only:

- original query
- retrieved stock candidates
- retrieved stock-unit candidates
- similarity scores

The LLM may:

- rank candidates
- choose one candidate
- explain ambiguity briefly

The LLM may not:

- invent a stock item
- invent a stock unit
- invent stock mappings
- select IDs outside the retrieved candidate set
