# DocVault — Semantic Document Search Platform

A full-stack application for uploading, storing, and semantically searching documents using vector embeddings. Built to demonstrate end-to-end system design across a polyglot stack.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3 + TypeScript + Pinia + Vite |
| API Gateway | Java 21 + Spring Boot 3 + Spring Security |
| Embeddings Service | Python 3.11 + FastAPI + sentence-transformers |
| Primary DB | PostgreSQL 15 (documents, users, metadata) |
| Vector DB | pgvector extension (semantic search index) |
| Auth | JWT (RS256) |
| Containerisation | Docker + Docker Compose |
| CI/CD | GitHub Actions |

## Features

- Upload PDF/TXT documents via drag-and-drop
- Automatic text extraction and chunking
- Sentence-level vector embeddings via a Python microservice
- Semantic search: ask questions in natural language, get ranked results
- User authentication with JWT
- Document management (list, view, delete)
- Full-text search fallback (PostgreSQL `tsvector`)

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Vue 3 Frontend                       │
│              TypeScript + Pinia + Vite                   │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / REST
┌──────────────────────▼──────────────────────────────────┐
│              Java Spring Boot API (Port 8080)            │
│   Controllers → Services → Repositories → PostgreSQL     │
│                     + pgvector                           │
└─────────────┬────────────────────────┬───────────────────┘
              │ JPA/Hibernate          │ HTTP
              │                       │
┌─────────────▼──────────┐  ┌─────────▼───────────────────┐
│   PostgreSQL 15         │  │  Python FastAPI (Port 8000)  │
│   - documents table     │  │  sentence-transformers       │
│   - users table         │  │  /embed  → float[] vectors   │
│   - chunks table        │  └─────────────────────────────┘
│   - pgvector index      │
└────────────────────────┘
```

## Getting Started

### Prerequisites

- Docker + Docker Compose
- Java 21 (for local dev without Docker)
- Node.js 20+ (for local dev without Docker)
- Python 3.11+ (for local dev without Docker)

### Run with Docker (recommended)

```bash
git clone https://github.com/Aurovindhya/docvault.git
cd docvault
docker compose up --build
```

- Frontend: http://localhost:5173
- Spring Boot API: http://localhost:8080
- Python Embeddings: http://localhost:8000
- pgAdmin (optional): http://localhost:5050

### Local Development

**1. Start PostgreSQL with pgvector**
```bash
docker compose up postgres -d
```

**2. Java backend**
```bash
cd backend-java
./mvnw spring-boot:run
```

**3. Python embeddings service**
```bash
cd backend-python
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

**4. Vue frontend**
```bash
cd frontend
npm install
npm run dev
```

## API Reference

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Get JWT token |

### Documents
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/documents` | List all documents for user |
| POST | `/api/documents` | Upload document (multipart) |
| GET | `/api/documents/{id}` | Get document by ID |
| DELETE | `/api/documents/{id}` | Delete document |

### Search
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/search?q={query}&limit={n}` | Semantic + full-text search |
| GET | `/api/search/semantic?q={query}` | Vector similarity search only |

### Embeddings (internal Python service)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/embed` | Embed a list of text chunks |
| GET | `/health` | Health check |

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Documents
CREATE TABLE documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    filename    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes  BIGINT,
    raw_text    TEXT,
    fts_vector  TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', raw_text)) STORED,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Chunks with vector embeddings
CREATE TABLE chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content     TEXT NOT NULL,
    embedding   vector(384)  -- all-MiniLM-L6-v2 dimensions
);

CREATE INDEX ON chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX ON documents USING GIN (fts_vector);
```

## Project Structure

```
docvault/
├── backend-java/          # Spring Boot API
│   └── src/main/java/com/docvault/
│       ├── controller/    # REST endpoints
│       ├── service/       # Business logic
│       ├── repository/    # JPA repositories
│       ├── model/         # JPA entities
│       └── config/        # Security, CORS, JWT
├── backend-python/        # FastAPI embeddings microservice
│   ├── main.py
│   ├── embedder.py
│   └── requirements.txt
├── frontend/              # Vue 3 + TypeScript
│   └── src/
│       ├── components/    # Reusable UI components
│       ├── views/         # Page-level components
│       ├── stores/        # Pinia state management
│       ├── api/           # Axios API client
│       └── types/         # TypeScript interfaces
├── docker-compose.yml
└── .github/workflows/     # CI/CD
```

## Design Decisions

**Why pgvector instead of a dedicated vector DB (Pinecone, Milvus)?**
For a document search use case at this scale, pgvector keeps the architecture simple, one DB for both relational and vector data. Dedicated vector DBs make sense at billions of vectors or when you need sub-10ms ANN at massive scale.

**Why a separate Python service for embeddings?**
`sentence-transformers` is a Python-native ecosystem. Calling it from Java via HTTP keeps each service in its strongest language, and makes the embedding model swappable without touching the Java codebase.

**Why Spring Boot + JPA over something lighter?**
Demonstrates enterprise-grade patterns: transactional service layer, repository abstraction, Spring Security with JWT filter chain, the kind of code that runs in production at scale.
