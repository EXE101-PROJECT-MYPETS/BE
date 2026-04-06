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
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private Long shopId;
    private MessageSenderType senderType;
    private Long senderCustomerId;
    private Long senderUserId;
    private String body;
    private OffsetDateTime createdAt;
}
