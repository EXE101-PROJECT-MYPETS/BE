package com.exe101.conversation.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.ScrollResponse;
import com.exe101.conversation.dto.CustomerConversationDTO;
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
import java.util.Map;

@RestController
@RequestMapping("/api/customer/conversations")
@RequiredArgsConstructor
public class CustomerConversationController {

    private final ConversationService conversationService;
    private final ConversationSocketPublisher socketPublisher;
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<CustomerConversationDTO>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(conversationService.getAllByUserId(getCurrentUserId(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerConversationDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.getByIdForUser(getCurrentUserId(principal), id));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ScrollResponse<MessageDTO>> getMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(conversationService.getMessagesForUser(getCurrentUserId(principal), id, cursor, size));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody MessageCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MessageDTO savedMessage = conversationService.sendMessageAsUser(getCurrentUserId(principal), id, request);
        socketPublisher.publishMessage(savedMessage);
        notificationService.publishChatMessage(savedMessage);
        return ResponseEntity.ok(savedMessage);
    }

    @PostMapping
    public ResponseEntity<CustomerConversationDTO> createConversation(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Object shopIdObj = payload.get("shopId");
        if (shopIdObj == null) {
            throw new ConversationValidationException("ShopIdRequired", "shopId là bắt buộc");
        }
        Long shopId = ((Number) shopIdObj).longValue();
        return ResponseEntity.ok(conversationService.getOrCreateConversation(getCurrentUserId(principal), shopId));
    }

    @PatchMapping("/{id}/user-read")
    public ResponseEntity<ReadReceiptDTO> markUserRead(
            @PathVariable Long id,
            @Valid @RequestBody ReadReceiptUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReadReceiptDTO readReceipt = conversationService.markUserRead(
                id,
                getCurrentUserId(principal),
                request.getLastReadMessageId()
        );
        socketPublisher.publishReadReceipt(readReceipt);
        return ResponseEntity.ok(readReceipt);
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new ConversationValidationException(
                    "AuthenticatedUserRequired",
                    "Cần đăng nhập để thực hiện chức năng này"
            );
        }
        return principal.getUser().getId();
    }
}
