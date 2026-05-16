package com.exe101.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiClientService {

    private final WebClient.Builder webClientBuilder;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.chat-model}")
    private String chatModel;

    @Value("${gemini.embedding-model}")
    private String embeddingModel;

    @Value("${gemini.base-url}")
    private String baseUrl;

    public String generateText(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        JsonNode response = webClientBuilder.build()
                .post()
                .uri(baseUrl + "/models/" + chatModel + ":generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return extractText(response);
    }

    public List<Double> embedText(String text) {
        Map<String, Object> body = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of(
                        "parts", List.of(
                                Map.of("text", text)
                        )
                ),
                "taskType", "RETRIEVAL_DOCUMENT",
                "outputDimensionality", 1536
        );

        JsonNode response = webClientBuilder.build()
                .post()
                .uri(baseUrl + "/models/" + embeddingModel + ":embedContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        JsonNode values = response == null ? null : response.path("embedding").path("values");
        if (values == null || !values.isArray() || values.isEmpty()) {
            throw new IllegalStateException("Gemini không trả về embedding hợp lệ");
        }
        List<Double> embedding = new ArrayList<>();
        for (JsonNode value : values) {
            embedding.add(value.asDouble());
        }
        return embedding;
    }

    private String extractText(JsonNode response) {
        JsonNode candidates = response == null ? null : response.path("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            return "";
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return "";
        }
        return parts.get(0).path("text").asText("");
    }
}
