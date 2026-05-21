package com.exe101.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiPetChatSocketEventDTO {
    private String eventType;
    private Long conversationId;
    private Long userId;
    private AiPetChatMessageDTO message;
    private AiPetChatResponse response;
}
