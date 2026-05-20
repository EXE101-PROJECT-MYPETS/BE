package com.exe101.subscription.controller;

import com.exe101.subscription.dto.SepayIpnResponse;
import com.exe101.subscription.dto.SepayWebhookRequest;
import com.exe101.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/sepay")
@RequiredArgsConstructor
@Slf4j
public class SepayIpnController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/ipn")
    public ResponseEntity<SepayIpnResponse> handleIpn(
            @RequestHeader Map<String, String> headers,
            @RequestBody SepayWebhookRequest request
    ) {
        if (!subscriptionService.isSepayWebhookAuthorized(headers)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new SepayIpnResponse(false, "Unauthorized"));
        }
        try {
            subscriptionService.handleSepayIpn(request);
        } catch (Exception ex) {
            log.error(
                    "SePay webhook processing failed but response stays 200. webhookId={}",
                    request != null ? request.getId() : null,
                    ex
            );
        }
        return ResponseEntity.ok(new SepayIpnResponse(true, "OK"));
    }
}
