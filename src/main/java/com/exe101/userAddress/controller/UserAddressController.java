package com.exe101.userAddress.controller;

import com.exe101.auth.exception.AuthAccessDeniedException;
import com.exe101.auth.model.UserPrincipal;
import com.exe101.userAddress.dto.UserAddressDTO;
import com.exe101.userAddress.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public ResponseEntity<List<UserAddressDTO>> getMyAddresses(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userAddressService.getAllByUserId(getCurrentUserId(principal)));
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
