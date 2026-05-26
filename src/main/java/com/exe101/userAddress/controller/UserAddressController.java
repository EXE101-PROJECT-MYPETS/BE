package com.exe101.userAddress.controller;

import com.exe101.auth.exception.AuthAccessDeniedException;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.userAddress.dto.UserAddressDTO;
import com.exe101.userAddress.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/users/me/address", "/api/users/me/addresses"})
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public ResponseEntity<List<UserAddressDTO>> getMyAddresses(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userAddressService.getAllByUserId(getCurrentUserId(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAddressDTO> getMyAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userAddressService.getById(getCurrentUserId(principal), id));
    }

    @PostMapping
    public ResponseEntity<UserAddressDTO> createMyAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserAddressDTO dto
    ) {
        return ResponseEntity.ok(userAddressService.create(getCurrentUserId(principal), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAddressDTO> updateMyAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserAddressDTO dto
    ) {
        return ResponseEntity.ok(userAddressService.update(getCurrentUserId(principal), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        userAddressService.delete(getCurrentUserId(principal), id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new AuthAccessDeniedException(
                    "AuthenticatedUserRequired",
                    "Cần đăng nhập để xem địa chỉ người dùng"
            );
        }
        return principal.getUser().getId();
    }
}
