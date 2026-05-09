package com.exe101.conversation.controller;

import com.exe101.conversation.dto.MessageCreateRequest;
import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.service.ConversationService;
import com.exe101.conversation.service.ConversationSocketPublisher;
import com.exe101.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ConversationSocketController {

    private final ConversationService conversationService;
    private final ConversationSocketPublisher socketPublisher;
    private final NotificationService notificationService;

    @MessageMapping("/conversations/{conversationId}/messages")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Valid MessageCreateRequest request
    ) {
        MessageDTO savedMessage = conversationService.sendMessage(conversationId, request);
        socketPublisher.publishMessage(savedMessage);
        notificationService.publishChatMessage(savedMessage);
    }
}
