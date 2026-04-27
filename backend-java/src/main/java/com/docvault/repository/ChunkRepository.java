package com.docvault.repository;

import com.docvault.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    List<Chunk> findByDocumentId(UUID documentId);

    /**
     * Semantic search using pgvector cosine distance.
     * The cast ::vector is required by the pgvector extension.
     *
     * Returns the top-k chunks most similar to the query embedding,
     * filtered to documents owned by the requesting user.
     */
    @Query(value = """
            SELECT c.*
            FROM chunks c
            JOIN documents d ON d.id = c.document_id
            WHERE d.user_id = :userId
            ORDER BY c.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Chunk> semanticSearch(
            @Param("userId") UUID userId,
            @Param("embedding") String embedding,   // passed as pgvector literal e.g. "[0.1,0.2,...]"
            @Param("limit") int limit
    );
}
