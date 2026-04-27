package com.docvault.controller;

import com.docvault.model.Document;
import com.docvault.model.User;
import com.docvault.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    public record DocumentResponse(
            UUID id,
            String filename,
            String contentType,
            long sizeBytes,
            OffsetDateTime createdAt
    ) {
        static DocumentResponse from(Document d) {
            return new DocumentResponse(
                    d.getId(), d.getFilename(), d.getContentType(),
                    d.getSizeBytes() != null ? d.getSizeBytes() : 0,
                    d.getCreatedAt()
            );
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(@AuthenticationPrincipal User user) {
        List<DocumentResponse> docs = documentService.listForUser(user)
                .stream().map(DocumentResponse::from).toList();
        return ResponseEntity.ok(docs);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        Document doc = documentService.upload(file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(doc));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        Document doc = documentService.getForUser(id, user);
        return ResponseEntity.ok(DocumentResponse.from(doc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        documentService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
