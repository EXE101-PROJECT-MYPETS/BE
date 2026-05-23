package com.exe101.ai.controller;

import com.exe101.ai.dto.AiPetChatRequest;
import com.exe101.ai.service.AiPetHealthChatService;
import com.exe101.ai.service.AiPetHealthSocketPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AiPetHealthSocketController {

    private final AiPetHealthChatService aiPetHealthChatService;
    private final AiPetHealthSocketPublisher aiPetHealthSocketPublisher;

    @MessageMapping("/ai/pet-health/chat")
    public void chat(@Valid AiPetChatRequest request) {
        AiPetHealthChatService.AiPetChatExecution execution = aiPetHealthChatService.chatAndCollect(request);
        aiPetHealthSocketPublisher.publishMessage(
                execution.response().getConversationId(),
                execution.userId(),
                execution.userEvent()
        );
        aiPetHealthSocketPublisher.publishMessage(
                execution.response().getConversationId(),
                execution.userId(),
                execution.assistantEvent()
        );
        aiPetHealthSocketPublisher.publishResponse(
                execution.userId(),
                execution.responseEvent()
        );
    }
}
