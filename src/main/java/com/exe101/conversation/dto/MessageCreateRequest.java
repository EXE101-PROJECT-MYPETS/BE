package com.exe101.conversation.dto;

import com.exe101.conversation.entity.MessageSenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageCreateRequest {

    @NotNull(message = "Loại người gửi không được để trống")
    private MessageSenderType senderType;

    private Long senderUserId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 5000, message = "Nội dung tin nhắn không được vượt quá 5000 ký tự")
    private String body;
}
