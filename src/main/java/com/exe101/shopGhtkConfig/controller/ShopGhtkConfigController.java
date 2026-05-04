package com.exe101.shopGhtkConfig.controller;

import com.exe101.shopGhtkConfig.dto.ShopGhtkConfigDTO;
import com.exe101.shopGhtkConfig.dto.ShopGhtkConfigTestResponse;
import com.exe101.shopGhtkConfig.service.ShopGhtkConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shops/{shopId}/ghtk-config")
@RequiredArgsConstructor
public class ShopGhtkConfigController {

    private final ShopGhtkConfigService shopGhtkConfigService;

    @GetMapping
    public ResponseEntity<ShopGhtkConfigDTO> getByShopId(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopGhtkConfigService.getByShopId(shopId));
    }

    @PutMapping
    public ResponseEntity<ShopGhtkConfigDTO> save(
            @PathVariable Long shopId,
            @Valid @RequestBody ShopGhtkConfigDTO dto
    ) {
        return ResponseEntity.ok(shopGhtkConfigService.save(shopId, dto));
    }

    @PostMapping("/test")
    public ResponseEntity<ShopGhtkConfigTestResponse> test(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopGhtkConfigService.test(shopId));
    }
}
