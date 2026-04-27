package com.docvault.service;

import com.docvault.model.Chunk;
import com.docvault.model.Document;
import com.docvault.model.User;
import com.docvault.repository.ChunkRepository;
import com.docvault.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;

    @Value("${app.embeddings.chunk-size:512}")
    private int chunkSize;

    @Value("${app.embeddings.chunk-overlap:64}")
    private int chunkOverlap;

    @Transactional(readOnly = true)
    public List<Document> listForUser(User user) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public Document getForUser(UUID documentId, User user) {
        return documentRepository.findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    @Transactional
    public Document upload(MultipartFile file, User user) throws IOException {
        String rawText = extractText(file);

        Document document = Document.builder()
                .user(user)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .rawText(rawText)
                .build();
        document = documentRepository.save(document);

        // Chunk the text and generate embeddings asynchronously
        List<String> chunks = splitIntoChunks(rawText, chunkSize, chunkOverlap);
        log.info("Document {} split into {} chunks", document.getId(), chunks.size());

        List<float[]> embeddings = embeddingClient.embed(chunks);

        List<Chunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            chunkEntities.add(Chunk.builder()
                    .document(document)
                    .chunkIndex(i)
                    .content(chunks.get(i))
                    .embedding(embeddings.get(i))
                    .build());
        }
        chunkRepository.saveAll(chunkEntities);

        log.info("Indexed {} embeddings for document {}", chunkEntities.size(), document.getId());
        return document;
    }

    @Transactional
    public void delete(UUID documentId, User user) {
        Document doc = getForUser(documentId, user);
        documentRepository.delete(doc);
    }

    // --- Text extraction ---

    private String extractText(MultipartFile file) throws IOException {
        String ct = file.getContentType();
        if (ct != null && ct.equals(MediaType.APPLICATION_PDF_VALUE)) {
            try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                return new PDFTextStripper().getText(pdf);
            }
        }
        // Treat as plain text for all other types
        return new String(file.getBytes());
    }

    // --- Chunking with overlap ---

    static List<String> splitIntoChunks(String text, int size, int overlap) {
        if (text == null || text.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            result.add(text.substring(start, end).strip());
            if (end == text.length()) break;
            start = end - overlap;
        }
        return result;
    }

    public static class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(UUID id) {
            super("Document not found: " + id);
        }
    }
}
