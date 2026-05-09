package com.exe101.conversation.controller;

import com.exe101.common.ScrollResponse;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.dto.MessageCreateRequest;
import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.dto.ReadReceiptDTO;
import com.exe101.conversation.dto.ReadReceiptUpdateRequest;
import com.exe101.conversation.exception.ConversationValidationException;
import com.exe101.conversation.service.ConversationService;
import com.exe101.conversation.service.ConversationSocketPublisher;
import com.exe101.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationSocketPublisher socketPublisher;
    private final NotificationService notificationService;

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
    public ResponseEntity<ScrollResponse<MessageDTO>> getMessages(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(conversationService.getMessages(shopId, id, cursor, size));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody MessageCreateRequest request
    ) {
        MessageDTO savedMessage = conversationService.sendMessage(shopId, id, request);
        socketPublisher.publishMessage(savedMessage);
        notificationService.publishChatMessage(savedMessage);
        return ResponseEntity.ok(savedMessage);
    }

    @PatchMapping("/{id}/user-read")
    public ResponseEntity<ReadReceiptDTO> markUserRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReadReceiptUpdateRequest request
    ) {
        ReadReceiptDTO readReceipt = conversationService.markUserRead(
                id,
                getCurrentUserId(principal),
                request.getLastReadMessageId()
        );
        socketPublisher.publishReadReceipt(readReceipt);
        return ResponseEntity.ok(readReceipt);
    }

    @PatchMapping("/{id}/shop-read")
    public ResponseEntity<ReadReceiptDTO> markShopRead(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReadReceiptUpdateRequest request
    ) {
        ReadReceiptDTO readReceipt = conversationService.markShopRead(
                shopId,
                id,
                getCurrentUserId(principal),
                request.getLastReadMessageId()
        );
        socketPublisher.publishReadReceipt(readReceipt);
        return ResponseEntity.ok(readReceipt);
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

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new ConversationValidationException(
                    "AuthenticatedUserRequired",
                    "Cần đăng nhập để cập nhật trạng thái đã đọc"
            );
        }
        return principal.getUser().getId();
    }
}
