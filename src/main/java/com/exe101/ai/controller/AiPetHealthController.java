package com.exe101.ai.controller;

import com.exe101.ai.dto.*;
import com.exe101.ai.service.AiPetHealthChatService;
import com.exe101.ai.service.AiPetHealthSocketPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/pet-health")
@RequiredArgsConstructor
public class AiPetHealthController {

    private final AiPetHealthChatService aiPetHealthChatService;
    private final AiPetHealthSocketPublisher aiPetHealthSocketPublisher;

    @PostMapping("/chat")
    public ResponseEntity<AiPetChatResponse> chat(@Valid @RequestBody AiPetChatRequest request) {
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
        return ResponseEntity.ok(execution.response());
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<AiPetChatConversationDTO>> getConversations(@RequestParam Long petId) {
        return ResponseEntity.ok(aiPetHealthChatService.getConversations(petId));
    }

    @PostMapping("/conversations/get-or-create")
    public ResponseEntity<AiPetChatConversationDTO> getOrCreateConversation(
            @Valid @RequestBody AiPetConversationRequest request
    ) {
        return ResponseEntity.ok(aiPetHealthChatService.getOrCreateConversation(request.getPetId()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<AiPetChatMessageDTO>> getMessages(@PathVariable Long conversationId) {
        return ResponseEntity.ok(aiPetHealthChatService.getMessages(conversationId));
    }
}
