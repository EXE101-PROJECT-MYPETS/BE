package com.exe101.email.controller;

import com.exe101.email.dto.EmailVerificationResponse;
import com.exe101.email.dto.RegisterEmailVerificationSendRequest;
import com.exe101.email.dto.RegisterEmailVerificationVerifyRequest;
import com.exe101.email.entity.EmailVerificationPurpose;
import com.exe101.email.entity.EmailVerificationToken;
import com.exe101.email.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/register/email-verification")
@RequiredArgsConstructor
public class RegisterEmailVerificationController {

    private final EmailService emailService;

    @PostMapping("/send-code")
    public ResponseEntity<EmailVerificationResponse> sendCode(
            @Valid @RequestBody RegisterEmailVerificationSendRequest request
    ) {
        EmailVerificationToken token = emailService.createAndSendRegisterVerificationCode(request.getEmail());
        return ResponseEntity.ok(new EmailVerificationResponse(
                true,
                token.getEmail(),
                token.getPurpose(),
                "Đã gửi mã xác thực đăng ký tới email"
        ));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<EmailVerificationResponse> verifyCode(
            @Valid @RequestBody RegisterEmailVerificationVerifyRequest request
    ) {
        EmailVerificationToken token = emailService.verifyCode(
                request.getEmail(),
                request.getCode(),
                EmailVerificationPurpose.REGISTER_VERIFY
        );
        return ResponseEntity.ok(new EmailVerificationResponse(
                true,
                token.getEmail(),
                token.getPurpose(),
                "Xác thực email đăng ký thành công"
        ));
    }
}
