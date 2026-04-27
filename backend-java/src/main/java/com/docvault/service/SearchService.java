package com.docvault.service;

import com.docvault.model.Chunk;
import com.docvault.model.Document;
import com.docvault.model.User;
import com.docvault.repository.ChunkRepository;
import com.docvault.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;

    public record SearchResult(
            UUID documentId,
            String filename,
            String excerpt,
            double score,
            String matchType   // "semantic" | "fulltext"
    ) {}

    /**
     * Hybrid search: runs semantic and full-text in parallel, deduplicates,
     * and returns results ranked by combined score.
     */
    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, User user, int limit) {
        List<SearchResult> semantic = semanticSearch(query, user, limit);
        List<SearchResult> fullText = fullTextSearch(query, user, limit);

        // Merge and deduplicate by documentId, keeping highest score
        Map<UUID, SearchResult> merged = new LinkedHashMap<>();
        for (SearchResult r : semantic) {
            merged.put(r.documentId(), r);
        }
        for (SearchResult r : fullText) {
            merged.merge(r.documentId(), r,
                    (existing, incoming) -> existing.score() >= incoming.score() ? existing : incoming);
        }

        return merged.values().stream()
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SearchResult> semanticSearch(String query, User user, int limit) {
        float[] queryEmbedding = embeddingClient.embed(List.of(query)).get(0);
        String vectorLiteral = toVectorLiteral(queryEmbedding);

        List<Chunk> chunks = chunkRepository.semanticSearch(user.getId(), vectorLiteral, limit);
        return chunks.stream().map(chunk -> new SearchResult(
                chunk.getDocument().getId(),
                chunk.getDocument().getFilename(),
                truncate(chunk.getContent(), 300),
                cosineSimilarity(queryEmbedding, chunk.getEmbedding()),
                "semantic"
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SearchResult> fullTextSearch(String query, User user, int limit) {
        List<Document> docs = documentRepository.fullTextSearch(user.getId(), query, limit);
        return docs.stream().map(doc -> new SearchResult(
                doc.getId(),
                doc.getFilename(),
                truncate(doc.getRawText(), 300),
                1.0,   // FTS doesn't produce a 0-1 similarity score; treat as max for ranking
                "fulltext"
        )).collect(Collectors.toList());
    }

    // --- Helpers ---

    private String toVectorLiteral(float[] embedding) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (float v : embedding) sj.add(String.valueOf(v));
        return sj.toString();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
