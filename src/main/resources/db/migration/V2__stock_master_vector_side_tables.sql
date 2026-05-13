CREATE TABLE IF NOT EXISTS stock_vector_embedding (
    stock_poid BIGINT PRIMARY KEY,
    stock_code VARCHAR(100),
    stock_name VARCHAR(1000),
    stock_name2 VARCHAR(100),
    stock_description VARCHAR(1000),
    active VARCHAR(1),
    deleted VARCHAR(1),
    serialized_json JSONB,
    embedding_vector VECTOR(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS stock_unit_vector_embedding (
    stock_unit_poid BIGINT PRIMARY KEY,
    stock_unit_code VARCHAR(20),
    stock_unit_name VARCHAR(100),
    active VARCHAR(1),
    deleted VARCHAR(1),
    serialized_json JSONB,
    embedding_vector VECTOR(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE stock_vector_embedding ADD COLUMN IF NOT EXISTS stock_code VARCHAR(100);
ALTER TABLE stock_vector_embedding ADD COLUMN IF NOT EXISTS stock_name VARCHAR(1000);
ALTER TABLE stock_vector_embedding ADD COLUMN IF NOT EXISTS stock_name2 VARCHAR(100);
ALTER TABLE stock_vector_embedding ADD COLUMN IF NOT EXISTS stock_description VARCHAR(1000);
ALTER TABLE stock_vector_embedding ADD COLUMN IF NOT EXISTS active VARCHAR(1);
ALTER TABLE stock_vector_embedding ADD COLUMN IF NOT EXISTS deleted VARCHAR(1);

ALTER TABLE stock_unit_vector_embedding ADD COLUMN IF NOT EXISTS stock_unit_code VARCHAR(20);
ALTER TABLE stock_unit_vector_embedding ADD COLUMN IF NOT EXISTS stock_unit_name VARCHAR(100);
ALTER TABLE stock_unit_vector_embedding ADD COLUMN IF NOT EXISTS active VARCHAR(1);
ALTER TABLE stock_unit_vector_embedding ADD COLUMN IF NOT EXISTS deleted VARCHAR(1);

CREATE INDEX IF NOT EXISTS idx_stock_vector_embedding_hnsw
    ON stock_vector_embedding USING hnsw (embedding_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_stock_unit_vector_embedding_hnsw
    ON stock_unit_vector_embedding USING hnsw (embedding_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_stock_vector_code ON stock_vector_embedding USING btree (lower(stock_code));
CREATE INDEX IF NOT EXISTS idx_stock_vector_name ON stock_vector_embedding USING btree (lower(stock_name));
CREATE INDEX IF NOT EXISTS idx_stock_unit_vector_code ON stock_unit_vector_embedding USING btree (lower(stock_unit_code));
CREATE INDEX IF NOT EXISTS idx_stock_unit_vector_name ON stock_unit_vector_embedding USING btree (lower(stock_unit_name));
