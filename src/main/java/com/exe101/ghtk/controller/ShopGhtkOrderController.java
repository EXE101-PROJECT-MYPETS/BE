package com.exe101.ghtk.controller;

import com.exe101.ghtk.dto.GhtkCancelShipmentResponse;
import com.exe101.ghtk.service.GhtkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/orders")
@RequiredArgsConstructor
public class ShopGhtkOrderController {

    private final GhtkOrderService ghtkOrderService;

    @DeleteMapping("/{orderId}/ghtk-cancel")
    public ResponseEntity<GhtkCancelShipmentResponse> cancelGhtkShipment(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ghtkOrderService.cancelShipment(shopId, orderId));
    }
}
