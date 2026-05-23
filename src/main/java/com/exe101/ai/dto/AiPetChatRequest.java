package com.exe101.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiPetChatRequest {

    private Long conversationId;

    @NotNull(message = "petId không được để trống")
    private Long petId;

    @NotBlank(message = "message không được để trống")
    private String message;
}
