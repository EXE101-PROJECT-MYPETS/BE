package com.exe101.conversation.service;

import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.dto.ReadReceiptDTO;
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

    public void publishReadReceipt(ReadReceiptDTO readReceipt) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + readReceipt.getConversationId() + "/read",
                readReceipt
        );
        messagingTemplate.convertAndSend(
                "/topic/shops/" + readReceipt.getShopId() + "/read",
                readReceipt
        );
    }
}
