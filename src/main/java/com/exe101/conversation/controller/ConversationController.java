package com.exe101.conversation.controller;

import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.service.ConversationService;
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

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getAll(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(conversationService.getAllByShopId(shopId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.getById(id));
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
