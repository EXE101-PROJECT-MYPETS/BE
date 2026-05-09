package com.exe101.conversation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReadReceiptUpdateRequest {

    @NotNull(message = "lastReadMessageId không được để trống")
    @Positive(message = "lastReadMessageId phải lớn hơn 0")
    private Long lastReadMessageId;
}
