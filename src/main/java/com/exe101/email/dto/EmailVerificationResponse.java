package com.exe101.email.dto;

import com.exe101.email.entity.EmailVerificationPurpose;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerificationResponse {
    private boolean success;
    private String email;
    private EmailVerificationPurpose purpose;
    private String message;
}
