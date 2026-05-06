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
public class ConversationDTO {
    private Long id;
    private Long shopId;
    private Long userId;
    private String userFullName;
    private String userPhone;
    private String userEmail;
    private String userAvatarUrlPreview;
    private Long lastMessageId;
    private String lastMessageBody;
    private MessageSenderType lastMessageSenderType;
    private OffsetDateTime lastMessageCreatedAt;
    private Long unreadCount;
    private Long shopLastReadMessageId;
    private Long userLastReadMessageId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
