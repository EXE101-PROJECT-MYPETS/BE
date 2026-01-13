package com.exe101.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
