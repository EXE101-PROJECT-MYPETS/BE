package com.exe101.ai.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiPetChatMessageDTO {
    private Long id;
    private String role;
    private String content;
    private JsonNode metadata;
    private OffsetDateTime createdAt;
}
