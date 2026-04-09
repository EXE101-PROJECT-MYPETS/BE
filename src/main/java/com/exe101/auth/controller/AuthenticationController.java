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
        return ResponseEntity.ok(authenticationService.authenticateShop(request));
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
}
