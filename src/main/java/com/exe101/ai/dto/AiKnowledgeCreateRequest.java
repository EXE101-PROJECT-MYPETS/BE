package com.exe101.ai.dto;

import com.exe101.ai.enums.AiKnowledgeSourceType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiKnowledgeCreateRequest {
    private String topic;

    @NotNull(message = "sourceType không được để trống")
    private AiKnowledgeSourceType sourceType;

    private Long sourceId;
    private String title;

    @NotBlank(message = "content không được để trống")
    private String content;

    private JsonNode metadata;
}
