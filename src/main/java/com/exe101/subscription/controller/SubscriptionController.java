package com.exe101.subscription.controller;

import com.exe101.common.PageResponse;
import com.exe101.subscription.dto.*;
import com.exe101.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/overview")
    public ResponseEntity<SubscriptionOverviewResponse> getOverview(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId
    ) {
        return ResponseEntity.ok(subscriptionService.getOverview(shopId));
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getActivePlans());
    }

    @PostMapping("/payments/sepay-qr")
    public ResponseEntity<SepayQrPaymentResponse> createSepayQrPayment(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @Valid @RequestBody SepayQrPaymentRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.createSepayQrPayment(shopId, request));
    }

    @GetMapping("/payments/current")
    public ResponseEntity<SepayQrPaymentResponse> getCurrentPendingPayment(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId
    ) {
        return ResponseEntity.ok(subscriptionService.getCurrentPendingPayment(shopId));
    }

    @GetMapping("/payments/{paymentId}/status")
    public ResponseEntity<SubscriptionPaymentStatusResponse> getPaymentStatus(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(subscriptionService.getPaymentStatus(shopId, paymentId));
    }

    @PostMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<SubscriptionCancelPaymentResponse> cancelPayment(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(subscriptionService.cancelPendingPayment(shopId, paymentId));
    }

    @GetMapping("/payments")
    public ResponseEntity<PageResponse<SubscriptionPaymentHistoryItemDTO>> getPayments(
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(subscriptionService.getPaymentHistory(shopId, page, size));
    }
}
