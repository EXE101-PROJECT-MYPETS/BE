package com.exe101.conversation.dto;

import com.exe101.conversation.entity.MessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerConversationDTO {
    private Long id;
    private Long shopId;
    private Long customerId;
    private String shopName;
    private String shopAvatarUrl;
    private MessageDTO lastMessage;
    private Long unreadCount;
    private OffsetDateTime createdAt;

    // Custom constructor for JPA query mapping
    public CustomerConversationDTO(
            Long id,
            Long shopId,
            Long customerId,
            String shopName,
            String shopAvatarUrl,
            Long lastMessageId,
            String lastMessageBody,
            MessageSenderType lastMessageSenderType,
            OffsetDateTime lastMessageCreatedAt,
            Long unreadCount,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.shopId = shopId;
        this.customerId = customerId;
        this.shopName = shopName;
        this.shopAvatarUrl = shopAvatarUrl;
        this.unreadCount = unreadCount;
        this.createdAt = createdAt;

        if (lastMessageId != null) {
            MessageDTO msg = new MessageDTO();
            msg.setId(lastMessageId);
            msg.setConversationId(id);
            msg.setShopId(shopId);
            msg.setSenderType(lastMessageSenderType);
            msg.setBody(lastMessageBody);
            msg.setCreatedAt(lastMessageCreatedAt);
            this.lastMessage = msg;
        }
    }
}
