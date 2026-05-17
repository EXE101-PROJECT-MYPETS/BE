package com.exe101.dashboard.controller;

import com.exe101.dashboard.dto.*;
import com.exe101.dashboard.service.ShopOwnerDashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/shop-owner/dashboard")
@RequiredArgsConstructor
public class ShopOwnerDashboardController {

    private final ShopOwnerDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        return ResponseEntity.ok(dashboardService.getSummary(shopId, year, month));
    }

    @GetMapping("/revenue-by-month")
    public ResponseEntity<List<MonthlyRevenueDTO>> getRevenueByMonth(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam @Min(2000) @Max(2100) int year
    ) {
        return ResponseEntity.ok(dashboardService.getRevenueByMonth(shopId, year));
    }

    @GetMapping("/orders-by-month")
    public ResponseEntity<List<MonthlyOrderDTO>> getOrdersByMonth(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam @Min(2000) @Max(2100) int year
    ) {
        return ResponseEntity.ok(dashboardService.getOrdersByMonth(shopId, year));
    }

    @GetMapping("/services-by-category")
    public ResponseEntity<List<ServiceCategoryStatDTO>> getServicesByCategory(
            @RequestHeader("X-Shop-Id") Long shopId
    ) {
        return ResponseEntity.ok(dashboardService.getServicesByCategory(shopId));
    }

    @GetMapping("/bookings-by-category")
    public ResponseEntity<List<BookingCategoryStatDTO>> getBookingsByCategory(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(dashboardService.getBookingsByCategory(shopId, year, month));
    }

    @GetMapping("/inventory-status")
    public ResponseEntity<InventoryStatusDTO> getInventoryStatus(
            @RequestHeader("X-Shop-Id") Long shopId
    ) {
        return ResponseEntity.ok(dashboardService.getInventoryStatus(shopId));
    }

    @GetMapping("/inventory-alerts")
    public ResponseEntity<InventoryAlertDashboardDTO> getInventoryAlerts(
            @RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return ResponseEntity.ok(dashboardService.getInventoryAlerts(shopId, limit));
    }
}
