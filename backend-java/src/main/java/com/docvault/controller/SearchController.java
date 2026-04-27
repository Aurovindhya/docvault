package com.docvault.controller;

import com.docvault.model.User;
import com.docvault.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<List<SearchService.SearchResult>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(searchService.search(q, user, Math.min(limit, 50)));
    }

    @GetMapping("/semantic")
    public ResponseEntity<List<SearchService.SearchResult>> semanticOnly(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(searchService.semanticSearch(q, user, Math.min(limit, 50)));
    }
}
