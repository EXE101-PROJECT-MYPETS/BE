package com.exe101.shop.controller;

import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.dto.ShopRegistrationEmailRequest;
import com.exe101.shop.dto.ShopRegistrationEmailResponse;
import com.exe101.shop.entity.ShopStatus;
import com.exe101.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
public class ShopAdminController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<List<ShopDTO>> getAll(@RequestParam(required = false) ShopStatus status) {
        return ResponseEntity.ok(shopService.getAllByStatus(status));
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<ShopDTO> getById(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.getByIdWithOwner(shopId));
    }

    @PostMapping("/{shopId}/email")
    public ResponseEntity<ShopRegistrationEmailResponse> sendRegistrationEmail(
            @PathVariable Long shopId,
            @Valid @RequestBody ShopRegistrationEmailRequest request
    ) {
        return ResponseEntity.ok(shopService.sendRegistrationEmail(shopId, request));
    }

    @PutMapping("/{shopId}/approve")
    public ResponseEntity<ShopDTO> approve(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.approve(shopId));
    }

    @PutMapping("/{shopId}/reject")
    public ResponseEntity<ShopDTO> reject(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.reject(shopId));
    }
}
