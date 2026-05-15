package com.exe101.ai.controller;

import com.exe101.ai.dto.AiPetChatConversationDTO;
import com.exe101.ai.dto.AiPetChatMessageDTO;
import com.exe101.ai.dto.AiPetChatRequest;
import com.exe101.ai.dto.AiPetChatResponse;
import com.exe101.ai.service.AiPetHealthChatService;
import com.exe101.ai.service.AiPetHealthSocketPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<AiPetChatMessageDTO>> getMessages(@PathVariable Long conversationId) {
        return ResponseEntity.ok(aiPetHealthChatService.getMessages(conversationId));
    }
}
