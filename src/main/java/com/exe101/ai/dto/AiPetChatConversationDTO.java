package com.exe101.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiPetChatConversationDTO {
    private Long id;
    private Long petId;
    private String petName;
    private String title;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
