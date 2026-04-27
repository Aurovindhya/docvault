"""
DocVault Embeddings Service
FastAPI microservice that wraps sentence-transformers for text embedding.
Called by the Java Spring Boot API after document upload.
"""

from contextlib import asynccontextmanager
from typing import List

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import logging

from embedder import Embedder

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Global embedder instance — loaded once on startup
embedder: Embedder | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global embedder
    logger.info("Loading sentence-transformers model...")
    embedder = Embedder()
    logger.info("Model loaded: %s | Dimension: %d", embedder.model_name, embedder.dimension)
    yield
    logger.info("Shutting down embeddings service")


app = FastAPI(
    title="DocVault Embeddings Service",
    description="Sentence-level vector embeddings via sentence-transformers",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)


class EmbedRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=512)


class EmbedResponse(BaseModel):
    embeddings: List[List[float]]
    model: str
    dimension: int


class HealthResponse(BaseModel):
    status: str
    model: str
    dimension: int


@app.post("/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest) -> EmbedResponse:
    """
    Embed a list of text strings.
    Returns one float[] vector per input text.
    """
    if embedder is None:
        raise HTTPException(status_code=503, detail="Embedder not initialised")

    if not request.texts:
        raise HTTPException(status_code=400, detail="texts list cannot be empty")

    vectors = embedder.embed(request.texts)
    return EmbedResponse(
        embeddings=vectors,
        model=embedder.model_name,
        dimension=embedder.dimension,
    )


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    if embedder is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    return HealthResponse(
        status="ok",
        model=embedder.model_name,
        dimension=embedder.dimension,
    )
