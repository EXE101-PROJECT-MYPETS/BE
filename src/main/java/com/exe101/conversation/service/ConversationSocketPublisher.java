package com.exe101.conversation.service;

import com.exe101.conversation.dto.MessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessage(MessageDTO message) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + message.getConversationId() + "/messages",
                message
        );
        messagingTemplate.convertAndSend(
                "/topic/shops/" + message.getShopId() + "/messages",
                message
        );
    }
}
