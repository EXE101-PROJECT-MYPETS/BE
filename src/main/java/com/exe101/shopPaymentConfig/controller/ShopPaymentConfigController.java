package com.exe101.shopPaymentConfig.controller;

import com.exe101.shopPaymentConfig.dto.ShopPaymentConfigDTO;
import com.exe101.shopPaymentConfig.service.ShopPaymentConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop-payment-configs")
@RequiredArgsConstructor
public class ShopPaymentConfigController {

    private final ShopPaymentConfigService shopPaymentConfigService;

    @GetMapping
    public ResponseEntity<List<ShopPaymentConfigDTO>> getAll(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(shopPaymentConfigService.getAllByShopId(shopId, active));
    }

    @GetMapping("/default")
    public ResponseEntity<ShopPaymentConfigDTO> getDefault(@RequestHeader("X-Shop-Id") Long shopId) {
        return ResponseEntity.ok(shopPaymentConfigService.getDefault(shopId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopPaymentConfigDTO> getById(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(shopPaymentConfigService.getById(shopId, id));
    }

    @PostMapping
    public ResponseEntity<ShopPaymentConfigDTO> create(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ShopPaymentConfigDTO dto
    ) {
        return ResponseEntity.ok(shopPaymentConfigService.create(shopId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShopPaymentConfigDTO> update(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id,
            @Valid @RequestBody ShopPaymentConfigDTO dto
    ) {
        return ResponseEntity.ok(shopPaymentConfigService.update(shopId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long id
    ) {
        shopPaymentConfigService.delete(shopId, id);
        return ResponseEntity.noContent().build();
    }
}
