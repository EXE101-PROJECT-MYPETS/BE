package com.exe101.fcm.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.fcm.dto.FcmTokenRequest;
import com.exe101.fcm.service.FirebaseMessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fcm-token")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FirebaseMessagingService firebaseMessagingService;

    @PostMapping
    public ResponseEntity<Void> saveToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody FcmTokenRequest request) {
        if (userPrincipal != null && userPrincipal.getUser() != null) {
            firebaseMessagingService.saveToken(userPrincipal.getUser().getId(), request.getToken());
        }
        return ResponseEntity.ok().build();
    }
}
