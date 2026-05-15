package com.exe101.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeCreateResponse {
    private UUID id;
    private String title;
    private String topic;
}
