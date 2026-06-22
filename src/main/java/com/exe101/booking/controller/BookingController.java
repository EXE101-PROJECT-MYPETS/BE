package com.exe101.booking.controller;

import com.exe101.booking.dto.*;
import com.exe101.booking.entity.BookingSource;
import com.exe101.booking.entity.BookingStatus;
import com.exe101.booking.service.BookingService;
import com.exe101.common.ScrollResponse;
import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.auth.model.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ScrollResponse<BookingListItemDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) BookingSource source,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate appointmentDate,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(bookingService.getAllForScroll(
                shopId,
                userId,
                customerId,
                customerName,
                status,
                source,
                createDate,
                appointmentDate,
                cursor,
                size
        ));
    }

    @GetMapping("/customer")
    public ResponseEntity<ScrollResponse<BookingListItemDTO>> getCustomerBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new IllegalStateException("Cần đăng nhập để thực hiện chức năng này");
        }
        return ResponseEntity.ok(bookingService.getAllForScroll(
                null,
                principal.getUser().getId(),
                null,
                null,
                status,
                null,
                null,
                null,
                cursor,
                size
        ));
    }

    @GetMapping("/by-day")
    public ResponseEntity<List<BookingListItemDTO>> getByCurrentDate(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate currentDate
    ) {
        return ResponseEntity.ok(bookingService.getByCurrentDate(shopId, currentDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingListItemDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<InvoiceDTO> getInvoice(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookingService.getInvoice(shopId, id));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<BookingCheckoutResponse> checkout(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody BookingCheckoutRequest request
    ) {
        return ResponseEntity.ok(bookingService.checkout(id, shopId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingListItemDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody BookingDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(bookingService.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookingListItemDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam(required = false) BookingStatus status,
            @RequestBody(required = false) BookingStatusUpdateDTO dto
    ) {
        BookingStatus targetStatus = status != null ? status : dto != null ? dto.getStatus() : null;
        return ResponseEntity.ok(bookingService.updateStatus(id, targetStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
