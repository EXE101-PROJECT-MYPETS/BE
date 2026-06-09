package com.exe101.commission.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.commission.dto.CommissionDTO;
import com.exe101.commission.dto.CommissionInvoiceDTO;
import com.exe101.commission.dto.CommissionPaymentInfoDTO;
import com.exe101.commission.dto.CommissionSummaryDTO;
import com.exe101.commission.entity.CommissionSourceType;
import com.exe101.commission.entity.CommissionStatus;
import com.exe101.commission.service.CommissionInvoiceService;
import com.exe101.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopCommissionController {

    private final CommissionInvoiceService commissionInvoiceService;

    @GetMapping("/commission-summary")
    public ResponseEntity<CommissionSummaryDTO> getSummary(
            @RequestHeader("X-Shop-Id") Long shopId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getSummary(shopId, getCurrentUserId(principal)));
    }

    @GetMapping("/commissions")
    public ResponseEntity<PageResponse<CommissionDTO>> getCommissions(
            @RequestHeader("X-Shop-Id") Long shopId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) CommissionSourceType sourceType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getShopCommissions(
                shopId,
                getCurrentUserId(principal),
                status,
                sourceType,
                from,
                to,
                page,
                size
        ));
    }

    @GetMapping("/commission-invoices")
    public ResponseEntity<PageResponse<CommissionInvoiceDTO>> getInvoices(
            @RequestHeader("X-Shop-Id") Long shopId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getShopInvoices(
                shopId,
                getCurrentUserId(principal),
                page,
                size
        ));
    }

    @GetMapping("/commission-invoices/{invoiceId}")
    public ResponseEntity<CommissionInvoiceDTO> getInvoice(
            @RequestHeader("X-Shop-Id") Long shopId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long invoiceId
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getShopInvoice(
                shopId,
                getCurrentUserId(principal),
                invoiceId
        ));
    }

    @GetMapping("/commission-invoices/{invoiceId}/payment-info")
    public ResponseEntity<CommissionPaymentInfoDTO> getPaymentInfo(
            @RequestHeader("X-Shop-Id") Long shopId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long invoiceId
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getShopInvoicePaymentInfo(
                shopId,
                getCurrentUserId(principal),
                invoiceId
        ));
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new IllegalArgumentException("Ban can dang nhap");
        }
        return principal.getUser().getId();
    }
}
