package com.exe101.conversation.controller;

import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.dto.MessageCreateRequest;
import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.service.ConversationService;
import com.exe101.conversation.service.ConversationSocketPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationSocketPublisher socketPublisher;

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getAll(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(conversationService.getAllByShopId(shopId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(conversationService.getById(shopId, id));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(conversationService.getMessages(shopId, id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody MessageCreateRequest request
    ) {
        MessageDTO savedMessage = conversationService.sendMessage(shopId, id, request);
        socketPublisher.publishMessage(savedMessage);
        return ResponseEntity.ok(savedMessage);
    }

    @PostMapping
    public ResponseEntity<ConversationDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ConversationDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(conversationService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConversationDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ConversationDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(conversationService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        conversationService.delete(shopId, id);
        return ResponseEntity.noContent().build();
    }
}
