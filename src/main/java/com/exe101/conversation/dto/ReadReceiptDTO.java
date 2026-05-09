package com.exe101.conversation.dto;

import com.exe101.conversation.entity.MessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptDTO {
    private Long conversationId;
    private Long shopId;
    private MessageSenderType readerType;
    private Long readerUserId;
    private Long lastReadMessageId;
    private Long shopLastReadMessageId;
    private Long userLastReadMessageId;
}
