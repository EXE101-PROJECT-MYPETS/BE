package com.exe101.booking.controller;

import com.exe101.booking.dto.BookingDTO;
import com.exe101.booking.dto.BookingListItemDTO;
import com.exe101.booking.dto.BookingStaffAssignmentDTO;
import com.exe101.booking.dto.BookingStatusUpdateDTO;
import com.exe101.booking.entity.BookingSource;
import com.exe101.booking.entity.BookingStatus;
import com.exe101.booking.service.BookingService;
import com.exe101.common.ScrollResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ScrollResponse<BookingListItemDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) BookingSource source,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(bookingService.getAllForScroll(
                shopId,
                customerId,
                customerName,
                status,
                source,
                cursor,
                size
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BookingDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody BookingDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(bookingService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody BookingDTO dto
    ) {
        dto.setShopId(shopId);
        return ResponseEntity.ok(bookingService.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookingDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam(required = false) BookingStatus status,
            @RequestBody(required = false) BookingStatusUpdateDTO dto
    ) {
        BookingStatus targetStatus = status != null ? status : dto != null ? dto.getStatus() : null;
        return ResponseEntity.ok(bookingService.updateStatus(id, targetStatus));
    }

    @PutMapping("/{id}/staff")
    public ResponseEntity<BookingDTO> updateAssignedStaffs(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @RequestBody BookingStaffAssignmentDTO dto
    ) {
        return ResponseEntity.ok(bookingService.updateAssignedStaffs(id, shopId, dto.getStaffUserIds()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
