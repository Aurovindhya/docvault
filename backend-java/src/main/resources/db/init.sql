-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Documents table with full-text search column
CREATE TABLE IF NOT EXISTS documents (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes   BIGINT NOT NULL DEFAULT 0,
    raw_text     TEXT,
    fts_vector   TSVECTOR GENERATED ALWAYS AS (
                     to_tsvector('english', COALESCE(raw_text, ''))
                 ) STORED,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Chunks table: stores text segments + their vector embeddings
-- Embedding dimension 384 matches all-MiniLM-L6-v2
CREATE TABLE IF NOT EXISTS chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index  INT NOT NULL,
    content      TEXT NOT NULL,
    embedding    vector(384),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_documents_user_id  ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_fts       ON documents USING GIN(fts_vector);
CREATE INDEX IF NOT EXISTS idx_chunks_document_id  ON chunks(document_id);

-- IVFFlat index for approximate nearest neighbour search (cosine distance)
-- lists = 100 is a good default for up to ~1M vectors
CREATE INDEX IF NOT EXISTS idx_chunks_embedding
    ON chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
