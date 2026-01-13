package com.exe101.auth.controller;

import com.exe101.auth.dto.*;
import com.exe101.auth.service.AuthenticationService;
import com.exe101.auth.service.JwtService;
import com.exe101.auth.service.RefreshTokenService;
import com.exe101.user.entity.User;
import com.exe101.user.repository.IUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final IUserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestBody RefreshRequest req
    ) {

        var rotated = refreshTokenService.rotate(req.getRefreshToken());

        User user = userRepository
                .findById(rotated.getUserId())
                .orElseThrow();

        String accessToken = jwtService.generateToken(user);

        return ResponseEntity.ok(
                new AuthenticationResponse(
                        accessToken,
                        user.getRole(),
                        rotated.getToken()
                )
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
