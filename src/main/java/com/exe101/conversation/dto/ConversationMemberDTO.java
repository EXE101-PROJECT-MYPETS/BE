package com.exe101.conversation.dto;

import com.exe101.conversation.entity.ConversationMemberType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberDTO {
    private Long id;
    private Long conversationId;
    private ConversationMemberType memberType;
    private Long customerId;
    private Long userId;
    private Long lastReadMessageId;
    private OffsetDateTime createdAt;
}
