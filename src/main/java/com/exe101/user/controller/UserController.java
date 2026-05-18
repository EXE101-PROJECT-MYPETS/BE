package com.exe101.user.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.user.dto.UserDTO;
import com.exe101.user.dto.UserProfileUpdateRequest;
import com.exe101.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/user", "/api/users"})
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(principal));
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute @Valid UserProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateCurrentUser(principal, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userService.getById(id));
    }
}
