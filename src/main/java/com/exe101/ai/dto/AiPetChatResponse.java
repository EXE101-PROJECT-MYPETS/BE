package com.exe101.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiPetChatResponse {
    private Long conversationId;
    private String answer;
    private String riskLevel;
    private Boolean shouldBookVet;
    private List<String> recommendedActions;
}
