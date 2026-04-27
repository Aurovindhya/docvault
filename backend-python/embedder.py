"""
Embedder: wraps sentence-transformers with batching and normalisation.
Model: all-MiniLM-L6-v2 (384-dim, fast, good quality for semantic search)
"""

import os
from typing import List

from sentence_transformers import SentenceTransformer
import numpy as np


class Embedder:
    """Thread-safe sentence embedding wrapper."""

    def __init__(self, model_name: str | None = None) -> None:
        self.model_name = model_name or os.getenv("MODEL_NAME", "all-MiniLM-L6-v2")
        self._model = SentenceTransformer(self.model_name)
        self.dimension: int = self._model.get_sentence_embedding_dimension()

    def embed(self, texts: List[str], batch_size: int = 64) -> List[List[float]]:
        """
        Embed a list of texts.

        Args:
            texts: input strings (non-empty, stripped)
            batch_size: number of texts per inference batch

        Returns:
            List of float lists, one per input text.
            Vectors are L2-normalised (unit vectors) for cosine similarity.
        """
        if not texts:
            return []

        # Strip and truncate inputs to 512 tokens (model limit)
        cleaned = [t.strip()[:2048] for t in texts]

        embeddings: np.ndarray = self._model.encode(
            cleaned,
            batch_size=batch_size,
            normalize_embeddings=True,   # unit vectors → cosine sim == dot product
            show_progress_bar=False,
        )

        return embeddings.tolist()
