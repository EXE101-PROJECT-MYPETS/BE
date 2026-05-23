package com.exe101.booking.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.booking.dto.BookingCreateRequest;
import com.exe101.booking.dto.BookingListItemDTO;
import com.exe101.booking.exception.BookingValidationException;
import com.exe101.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops/{shopId}/bookings")
@RequiredArgsConstructor
public class ShopBookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingListItemDTO> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long shopId,
            @Valid @RequestBody BookingCreateRequest request
    ) {
        return ResponseEntity.ok(bookingService.create(shopId, getCurrentUserId(principal), request));
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new BookingValidationException(
                    "AuthenticatedUserRequired",
                    "Cần đăng nhập để tạo booking"
            );
        }
        return principal.getUser().getId();
    }
}
