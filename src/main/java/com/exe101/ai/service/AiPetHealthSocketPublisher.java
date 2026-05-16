package com.exe101.ai.service;

import com.exe101.ai.dto.AiPetChatSocketEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiPetHealthSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessage(Long conversationId, Long userId, AiPetChatSocketEventDTO event) {
        messagingTemplate.convertAndSend(
                "/topic/ai/pet-health/conversations/" + conversationId + "/messages",
                event
        );
        if (userId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/users/" + userId + "/ai/pet-health/messages",
                    event
            );
        }
    }

    public void publishResponse(Long userId, AiPetChatSocketEventDTO event) {
        if (userId == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/users/" + userId + "/ai/pet-health/responses",
                event
        );
    }
}
