CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(100) NOT NULL,
    stock_code VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    serialized_json JSONB,
    embedding_vector VECTOR(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_product_code UNIQUE (product_code),
    CONSTRAINT uk_inventory_stock_code UNIQUE (stock_code)
);

CREATE TABLE unit (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    unit_code VARCHAR(100) NOT NULL,
    unit_name VARCHAR(255) NOT NULL,
    description TEXT,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    serialized_json JSONB,
    embedding_vector VECTOR(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_unit_inventory_code UNIQUE (inventory_id, unit_code)
);

CREATE TABLE compatibility_rules (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    unit_id BIGINT NOT NULL REFERENCES unit(id) ON DELETE CASCADE,
    is_valid BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_compatibility_inventory_unit UNIQUE (inventory_id, unit_id)
);

CREATE TABLE synonym (
    id BIGSERIAL PRIMARY KEY,
    synonym VARCHAR(255) NOT NULL,
    actual_value VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    confidence NUMERIC(5,4) NOT NULL DEFAULT 1.0,
    CONSTRAINT uk_synonym_type_synonym UNIQUE (type, synonym)
);

CREATE TABLE semantic_cache (
    id BIGSERIAL PRIMARY KEY,
    query_hash VARCHAR(128) NOT NULL UNIQUE,
    query_text TEXT NOT NULL,
    embedding_vector VECTOR(1536),
    response_json JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE llm_resolution_cache (
    id BIGSERIAL PRIMARY KEY,
    ambiguity_hash VARCHAR(128) NOT NULL UNIQUE,
    model VARCHAR(128) NOT NULL,
    request_json JSONB NOT NULL,
    response_json JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_embedding_hnsw
    ON inventory USING hnsw (embedding_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_unit_embedding_hnsw
    ON unit USING hnsw (embedding_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_inventory_product_name ON inventory USING btree (lower(product_name));
CREATE INDEX idx_inventory_stock_code ON inventory USING btree (lower(stock_code));
CREATE INDEX idx_unit_name ON unit USING btree (lower(unit_name));
CREATE INDEX idx_unit_code ON unit USING btree (lower(unit_code));
CREATE INDEX idx_compatibility_lookup ON compatibility_rules (inventory_id, unit_id, is_valid, priority);
CREATE INDEX idx_synonym_lookup ON synonym (type, lower(synonym));
CREATE INDEX idx_semantic_cache_expiry ON semantic_cache (expires_at);
CREATE INDEX idx_llm_resolution_cache_expiry ON llm_resolution_cache (expires_at);
