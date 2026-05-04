package com.exe101.ghtk.controller;

import com.exe101.ghtk.dto.GhtkSubmitOrderRequest;
import com.exe101.ghtk.dto.GhtkSubmitOrderResponse;
import com.exe101.ghtk.service.GhtkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ghtk/orders")
@RequiredArgsConstructor
public class GhtkOrderController {

    private final GhtkOrderService ghtkOrderService;

    @PostMapping("/{orderId}/submit")
    public ResponseEntity<GhtkSubmitOrderResponse> submitOrder(
            @RequestHeader("X-Shop-Id") Long shopId,
            @PathVariable Long orderId,
            @RequestBody(required = false) @Valid GhtkSubmitOrderRequest request
    ) {
        return ResponseEntity.ok(ghtkOrderService.submitOrder(shopId, orderId, request));
    }
}
