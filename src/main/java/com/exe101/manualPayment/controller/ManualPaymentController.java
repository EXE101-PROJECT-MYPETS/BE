package com.exe101.manualPayment.controller;

import com.exe101.manualPayment.dto.ManualPaymentConfirmRequest;
import com.exe101.manualPayment.dto.ManualPaymentConfirmResponse;
import com.exe101.manualPayment.service.ManualPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class ManualPaymentController {

    private final ManualPaymentService manualPaymentService;

    @PostMapping("/manual-confirm")
    public ResponseEntity<ManualPaymentConfirmResponse> confirmPayment(
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody ManualPaymentConfirmRequest request
    ) {
        return ResponseEntity.ok(manualPaymentService.confirmPayment(shopId, request));
    }
}
