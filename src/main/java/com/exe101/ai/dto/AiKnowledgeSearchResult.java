package com.exe101.ai.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeSearchResult {
    private UUID id;
    private String topic;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String content;
    private JsonNode metadata;
    private Double similarity;
}
