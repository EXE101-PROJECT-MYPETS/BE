package com.exe101.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiKnowledgeSearchTestRequest {
    @NotBlank(message = "query không được để trống")
    private String query;
    private Integer limit = 5;
}
