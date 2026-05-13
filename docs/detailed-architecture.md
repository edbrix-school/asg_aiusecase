# In-Depth Architecture & Flow Diagrams - ags-ai-usecase

This document provides a detailed view of the `ags-ai-usecase` service architecture, request processing flows, and system integrations.

## 1. System Component Architecture

The service follows a modern Spring Boot architecture integrated with AI services (OpenAI) and a vector-enabled database (pgvector).

```mermaid
graph TB
    subgraph ClientLayer [Client Layer]
        UI[Web/Mobile UI]
        ExternalAPI[External Service]
    end

    subgraph AppService [Spring Boot Application]
        direction TB
        Controller[Controllers: Search, Inventory]
        SearchSvc[Search Service Orchestrator]
        BRE[Business Rule Engine]
        LLMRes[LLM Resolution Service]
        EmbedSvc[Embedding Service]
        CacheSvc[Redis Cache Service]
        VectorRepo[Vector Repository]
        MQConsumer[RabbitMQ Event Consumer]
    end

    subgraph DataStorage [Data & Vector Storage]
        Postgres[(PostgreSQL + pgvector)]
        Redis[(Redis Cache)]
        OracleDB[(External Oracle/Legacy DB)]
    end

    subgraph AIServices [AI & External Providers]
        OpenAI_Embed[OpenAI: text-embedding-3-small]
        OpenAI_Chat[OpenAI: gpt-4o / gpt-4.1-nano]
    end

    subgraph Messaging [Event Bus]
        RabbitMQ[RabbitMQ]
    end

    UI --> Controller
    ExternalAPI --> Controller
    Controller --> SearchSvc
    SearchSvc --> CacheSvc
    SearchSvc --> EmbedSvc
    SearchSvc --> VectorRepo
    SearchSvc --> BRE
    SearchSvc --> LLMRes
    
    BRE --> VectorRepo
    LLMRes --> OpenAI_Chat
    EmbedSvc --> OpenAI_Embed
    
    VectorRepo --> Postgres
    CacheSvc --> Redis
    
    MQConsumer --> RabbitMQ
    MQConsumer --> OracleDB
    MQConsumer --> EmbedSvc
    MQConsumer --> Postgres
    
    ExternalAPI -- Inventory Updates --> RabbitMQ
```

---

## 2. Detailed Search Flow (Sequence)

This diagram shows the complete lifecycle of a `/search` request, including multi-layer caching and RAG-based ambiguity resolution.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant S as SearchService
    participant R as Redis Cache
    participant E as Embedding Service
    participant AI as OpenAI API
    participant V as pgvector (Postgres)
    participant BRE as Business Rule Engine
    participant L as LLM Resolution (RAG)

    C->>S: POST /search (query, options)
    
    S->>S: Normalize Query & Generate Exact Hash
    
    S->>R: Lookup Final Cache (Full Response)
    alt Final Cache Hit
        R-->>S: Return SearchResponse
        S-->>C: reasoningSource=CACHE
    else Final Cache Miss
        S->>R: Lookup Semantic Cache
        alt Semantic Cache Hit
            R-->>S: Return SearchResponse
            S-->>C: reasoningSource=CACHE
        end
    end

    Note over S,AI: Phase 1: Semantic Understanding
    S->>E: embed(query)
    E->>AI: Request text-embedding-3-small
    AI-->>E: Return float[] vector
    E-->>S: Return query vector

    Note over S,V: Phase 2: Vector Search
    par Vector Search (Inventories)
        S->>V: cosine_similarity search (stock_vector_embedding)
    and Vector Search (Units)
        S->>V: cosine_similarity search (stock_unit_vector_embedding)
    end
    V-->>S: Return Top-K matches (IDs + Scores)

    Note over S,BRE: Phase 3: Business Rule Evaluation
    S->>BRE: evaluate(query, vectorMatches, parsedUnit)
    BRE->>BRE: Resolve Synonyms
    BRE->>BRE: Find Exact Matches (Stock Code/Name)
    BRE->>BRE: Calculate Confidence Scores (Formula below)
    BRE-->>S: List<ResolvedCandidate>

    Note over S,L: Phase 4: Ambiguity Resolution (RAG)
    alt isAmbiguous && allowLlm
        S->>L: resolve(query, candidates)
        L->>AI: Chat Completion (Query + Candidate Metadata)
        AI-->>L: JSON {stockPoid, stockUnitPoid, confidence, reason}
        L-->>S: Validated Selected Candidate
        S-->>C: reasoningSource=LLM
    else Low Ambiguity or LLM Disabled
        S->>S: Select Best by Confidence
        S-->>C: reasoningSource=VECTOR
    end

    S->>R: Populate Caches (Final & Semantic)
```

---

## 3. Asynchronous Embedding Pipeline

How the system keeps the vector database synchronized with the main inventory system.

```mermaid
flowchart LR
    External[External System] -- "Stock Update" --> MQ[RabbitMQ]
    subgraph Worker [Embedding Worker]
        Consume[Event Consumer]
        Fetch[Fetch Full Data from DB]
        Compact[Compact to JSON String]
        GenEmbed[Generate Embedding via OpenAI]
        Upsert[Upsert pgvector table]
    end
    MQ --> Consume
    Consume --> Fetch
    Fetch --> Compact
    Compact --> GenEmbed
    GenEmbed --> Upsert
    Upsert --> CacheClear[Invalidate Redis Cache]
```

---

## 4. Core Data Model

The system operates on two primary entities: **Stock Master** and **Stock Unit**, with corresponding vector embeddings stored in pgvector.

### 4.1 Entities
- **InventoryEntity (Stock Master)**:
  - `stockPoid`: Primary key (Long).
  - `stockCode`: Unique identifier (e.g., SKU).
  - `stockName`: Primary search target.
  - `stockDescription`: Context for embeddings.
- **UnitEntity (Stock Unit)**:
  - `stockUnitPoid`: Primary key (Long).
  - `stockUnitCode`: Short code (e.g., "KG", "PCS").
  - `stockUnitName`: Full name (e.g., "Kilograms").

### 4.2 Vector Tables (pgvector)
- `stock_vector_embedding`: Stores `stockPoid` + `embedding` (vector 1536).
- `stock_unit_vector_embedding`: Stores `stockUnitPoid` + `embedding` (vector 1536).

---

## 5. Confidence Scoring Logic

The `ConfidenceCalculator` uses a weighted formula to rank candidates:

**Score Formula:**
$$Score = (InvSim \times 0.58) + (UnitSim \times 0.22) + ExactBoost + UnitBoost + SynonymBonus + CompatibilityBonus$$

| Component | Weight / Value | Description |
| :--- | :--- | :--- |
| **Inventory Similarity** | 0.58 | Cosine similarity from pgvector for Stock Master. |
| **Unit Similarity** | 0.22 | Cosine similarity from pgvector for Stock Unit. |
| **Exact Boost** | +0.18 | Granted if Query contains exact Stock Code or Name. |
| **Unit Boost** | +0.12 | Granted if Query mentions the specific Unit (e.g., "KG", "PCS"). |
| **Synonym Bonus** | +0.05 | Granted if a synonym was used for matching. |
| **Compatibility** | +0.08 / -0.40 | Heavy penalty if the unit is incompatible with the stock category. |

---

## 5. Observability Stack

The system implements a full "LPG" stack (Loki, Prometheus, Grafana) + Jaeger for distributed tracing.

```mermaid
graph LR
    App[ags-ai-usecase] -- "Traces (OTLP/gRPC)" --> OTel[OTel Collector]
    App -- "Metrics (Micrometer)" --> Prom[Prometheus]
    App -- "Logs (Logstash JSON)" --> Promtail[Promtail]
    
    OTel --> Jaeger[Jaeger]
    Promtail --> Loki[Loki]
    
    Jaeger --> Grafana[Grafana]
    Prom --> Grafana
    Loki --> Grafana
    
    subgraph Dashboard [Monitoring]
        Grafana
    end
```

- **Metrics**: Search latency, token usage, cache hit ratios, confidence distribution.
- **Traces**: End-to-end tracing of search requests including OpenAI API calls and Vector DB queries.
- **Logs**: Structured JSON logs correlated with trace IDs for easy debugging.
