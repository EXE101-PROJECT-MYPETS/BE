package com.exe101.fcm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FcmTokenRequest {
    @NotBlank(message = "Token cannot be empty")
    private String token;
}
