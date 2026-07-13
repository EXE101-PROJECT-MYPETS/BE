package com.exe101.commission.controller;

import com.exe101.commission.dto.CommissionDTO;
import com.exe101.commission.dto.CommissionInvoiceDTO;
import com.exe101.commission.dto.CommissionInvoiceGenerateRequest;
import com.exe101.commission.dto.CommissionInvoiceGenerateResponse;
import com.exe101.commission.dto.AdminCommissionCollectionStatus;
import com.exe101.commission.dto.AdminCommissionMonthlyReportDTO;
import com.exe101.commission.dto.AdminShopMonthlyCommissionDetailDTO;
import com.exe101.commission.service.CommissionInvoiceService;
import com.exe101.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCommissionController {

    private final CommissionInvoiceService commissionInvoiceService;

    @GetMapping("/commissions")
    public ResponseEntity<PageResponse<CommissionDTO>> getCommissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getAdminCommissions(page, size));
    }

    @GetMapping("/commission-invoices")
    public ResponseEntity<PageResponse<CommissionInvoiceDTO>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getAdminInvoices(page, size));
    }

    @GetMapping("/commission-reports/monthly")
    public ResponseEntity<AdminCommissionMonthlyReportDTO> getMonthlyReport(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AdminCommissionCollectionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getAdminMonthlyReport(
                month,
                keyword,
                status,
                page,
                size
        ));
    }

    @GetMapping("/commission-reports/monthly/{shopId}")
    public ResponseEntity<AdminShopMonthlyCommissionDetailDTO> getMonthlyShopDetail(
            @PathVariable Long shopId,
            @RequestParam(required = false) String month
    ) {
        return ResponseEntity.ok(commissionInvoiceService.getAdminShopMonthlyDetail(shopId, month));
    }

    @PostMapping("/commission-invoices/generate")
    public ResponseEntity<CommissionInvoiceGenerateResponse> generateInvoices(
            @Valid @RequestBody(required = false) CommissionInvoiceGenerateRequest request
    ) {
        List<CommissionInvoiceDTO> invoices;
        if (request != null && request.getPeriodFrom() != null && request.getPeriodTo() != null) {
            invoices = commissionInvoiceService
                    .generateInvoiceForPeriod(request.getPeriodFrom(), request.getPeriodTo(), request.getShopId())
                    .stream()
                    .map(invoice -> commissionInvoiceService.toInvoiceDTO(invoice, false))
                    .toList();
        } else {
            invoices = commissionInvoiceService.generateInvoicesForClosedPeriodIfNeeded().stream()
                    .map(invoice -> commissionInvoiceService.toInvoiceDTO(invoice, false))
                    .toList();
        }
        return ResponseEntity.ok(new CommissionInvoiceGenerateResponse(invoices.size(), invoices));
    }

    @PostMapping("/commission-invoices/{id}/mark-paid")
    public ResponseEntity<CommissionInvoiceDTO> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(commissionInvoiceService.markInvoicePaid(id));
    }

    @PostMapping("/commission-invoices/{id}/cancel")
    public ResponseEntity<CommissionInvoiceDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(commissionInvoiceService.cancelInvoice(id));
    }
}
