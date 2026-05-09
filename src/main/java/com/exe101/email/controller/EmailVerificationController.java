package com.exe101.email.controller;

import com.exe101.email.dto.EmailVerificationResponse;
import com.exe101.email.dto.EmailVerificationSendRequest;
import com.exe101.email.dto.EmailVerificationVerifyRequest;
import com.exe101.email.entity.EmailVerificationToken;
import com.exe101.email.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailService emailService;

    @PostMapping("/send-code")
    public ResponseEntity<EmailVerificationResponse> sendCode(
            @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        EmailVerificationToken token = emailService.createAndSendVerificationCode(
                request.getUserId(),
                request.getEmail(),
                request.getPurpose()
        );
        return ResponseEntity.ok(new EmailVerificationResponse(
                true,
                token.getEmail(),
                token.getPurpose(),
                "Đã gửi mã xác thực tới email"
        ));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<EmailVerificationResponse> verifyCode(
            @Valid @RequestBody EmailVerificationVerifyRequest request
    ) {
        EmailVerificationToken token = emailService.verifyCode(
                request.getEmail(),
                request.getCode(),
                request.getPurpose()
        );
        return ResponseEntity.ok(new EmailVerificationResponse(
                true,
                token.getEmail(),
                token.getPurpose(),
                "Xác thực email thành công"
        ));
    }
}
