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
    Vector --> Postgres["PostgreSQL"]
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
            par inventory search
                API->>V: inventory cosine search
            and unit search
                API->>V: unit cosine search
            end
        end
        API->>B: exact, synonym, unit, compatibility, confidence
        alt high confidence
            B-->>API: selected validated match
            API-->>C: reasoningSource=VECTOR
        else ambiguous
            API->>L: one batched LLM request with allowed candidates
            L-->>API: selected inventoryId/unitId
            API->>B: validate selected IDs and compatibility again
            API-->>C: reasoningSource=LLM or VECTOR fallback
        end
    end
```

## Async Embedding Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant API as Inventory/Unit API
    participant DB as PostgreSQL
    participant MQ as RabbitMQ
    participant W as Embedding Worker
    participant AI as OpenAI Embeddings
    participant R as Redis

    C->>API: POST /inventory or POST /unit
    API->>DB: save relational data and compact serialized JSON
    API->>MQ: publish durable event
    API-->>C: 201 Created
    MQ->>W: consume event
    W->>AI: generate embedding from compact JSON
    W->>DB: update embedding_vector
    W->>R: increment cache version
```

## Hallucination Boundary

The LLM receives only:

- original query
- retrieved inventory candidates
- retrieved unit candidates
- similarity scores
- valid compatibility mappings

The LLM may:

- rank candidates
- choose one candidate
- explain ambiguity briefly

The LLM may not:

- invent a product
- invent a unit
- invent stock mappings
- invent compatibility rules
- override an invalid compatibility rule
