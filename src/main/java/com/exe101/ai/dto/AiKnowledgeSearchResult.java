package com.exe101.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeSearchResult {
    private UUID id;
    private String topic;
    private String sourceType;
    private String sourceId;
    private String title;
    private String content;
    private String metadata;
    private double vectorScore;
    private double keywordScore;
    private double hybridScore;
    private double rerankScore;
}
