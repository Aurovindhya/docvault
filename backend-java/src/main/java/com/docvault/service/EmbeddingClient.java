package com.docvault.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for the Python FastAPI embeddings microservice.
 * Sends text chunks and receives float[] vectors back.
 */
@Slf4j
@Service
public class EmbeddingClient {

    private final RestTemplate restTemplate;
    private final String serviceUrl;

    public EmbeddingClient(
            RestTemplate restTemplate,
            @Value("${app.embeddings.service-url}") String serviceUrl
    ) {
        this.restTemplate = restTemplate;
        this.serviceUrl = serviceUrl;
    }

    /**
     * Embeds a list of text strings.
     *
     * @param texts list of text chunks
     * @return list of float[] vectors (one per input text)
     */
    public List<float[]> embed(List<String> texts) {
        if (texts.isEmpty()) return List.of();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("texts", texts);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.debug("Calling embeddings service for {} chunks", texts.size());

        ResponseEntity<EmbedResponse> response = restTemplate.exchange(
                serviceUrl + "/embed",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<EmbedResponse>() {}
        );

        if (response.getBody() == null) {
            throw new EmbeddingException("Empty response from embeddings service");
        }

        return response.getBody().embeddings();
    }

    public record EmbedResponse(List<float[]> embeddings) {}

    public static class EmbeddingException extends RuntimeException {
        public EmbeddingException(String msg) { super(msg); }
    }
}
