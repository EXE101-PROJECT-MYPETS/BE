package com.exe101.conversation.dto;

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
    private Long customerId;
    private OffsetDateTime createdAt;
}
