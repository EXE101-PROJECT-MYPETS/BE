package com.exe101.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiPetConversationRequest {

    @NotNull(message = "petId không được để trống")
    private Long petId;
}
