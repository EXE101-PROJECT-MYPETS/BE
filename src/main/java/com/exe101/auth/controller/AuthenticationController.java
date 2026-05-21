package com.exe101.auth.controller;

import com.exe101.auth.dto.*;
import com.exe101.auth.service.AuthenticationService;
import com.exe101.auth.service.RefreshTokenService;
import com.exe101.auth.service.ShopOwnerRegistrationService;
import com.exe101.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final ShopOwnerRegistrationService shopOwnerRegistrationService;


    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthenticationResponse> register(
            @ModelAttribute @Valid RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping(value = "/shop-owner/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShopOwnerRegistrationResponse> registerShopOwner(
            @ModelAttribute @Valid ShopOwnerRegisterRequest request
    ) {
        return ResponseEntity.ok(shopOwnerRegistrationService.register(request));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<AuthenticationResponse> customerLogin(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticateCustomer(request));
    }

    @PostMapping("/shop/login")
    public ResponseEntity<AuthenticationResponse> shopLogin(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticateShopOrAdmin(request));
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestBody RefreshRequest req
    ) {
        return ResponseEntity.ok(
                authenticationService.refreshToken(req.getRefreshToken())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody LogoutRequest request
    ) {
        refreshTokenService.revokeByToken(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    // log out all device
    @PostMapping("/logout-all")
    public void logoutAll(Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        refreshTokenService.revokeAllByUser(userId);
    }

    // ── Forgot Password Flow ────────────────────────────────────────────

    @PostMapping("/customer/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(authenticationService.forgotPassword(request));
    }

    @PostMapping("/customer/verify-otp-forgot-password")
    public ResponseEntity<?> verifyOtpForgotPassword(
            @Valid @RequestBody VerifyOtpForgotPasswordRequest request
    ) {
        authenticationService.verifyOtpForgotPassword(request);
        return ResponseEntity.ok().body(
                java.util.Map.of("message", "Mã OTP hợp lệ. Bạn có thể tiếp tục đặt lại mật khẩu.")
        );
    }

    @PostMapping("/customer/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok().body(
                java.util.Map.of("message", "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.")
        );
    }

    @PostMapping("/customer/google-login")
    public ResponseEntity<AuthenticationResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(authenticationService.googleLogin(request));
    }

    @PostMapping("/customer/facebook-login")
    public ResponseEntity<AuthenticationResponse> facebookLogin(
            @Valid @RequestBody FacebookLoginRequest request
    ) {
        return ResponseEntity.ok(authenticationService.facebookLogin(request));
    }
}
