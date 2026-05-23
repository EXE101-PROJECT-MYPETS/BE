package com.exe101.shop.controller;

import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.dto.ShopProfileUpdateRequest;
import com.exe101.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop-owner/shop-profile")
@RequiredArgsConstructor
public class ShopOwnerProfileController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<ShopDTO> getProfile(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(shopService.getById(shopId));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ShopDTO> updateProfile(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ShopDTO dto
    ) {
        return ResponseEntity.ok(shopService.update(shopId, dto));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShopDTO> updateProfileMultipart(
            @RequestHeader("X-Shop-Id") Long shopId,
            @ModelAttribute @Valid ShopProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(shopService.update(shopId, request));
    }
}
