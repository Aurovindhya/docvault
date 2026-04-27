package com.docvault.repository;

import com.docvault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Full-text search using PostgreSQL tsvector.
     * Returns documents ranked by relevance.
     */
    @Query(value = """
            SELECT d.*
            FROM documents d
            WHERE d.user_id = :userId
              AND d.fts_vector @@ plainto_tsquery('english', :query)
            ORDER BY ts_rank(d.fts_vector, plainto_tsquery('english', :query)) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Document> fullTextSearch(
            @Param("userId") UUID userId,
            @Param("query") String query,
            @Param("limit") int limit
    );
}
