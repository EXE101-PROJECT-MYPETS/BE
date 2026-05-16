package com.exe101.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiEmbeddingService {

    private final GeminiClientService geminiClientService;

    public List<Double> embed(String text) {
        return geminiClientService.embedText(text);
    }

    public String toPgVectorString(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
