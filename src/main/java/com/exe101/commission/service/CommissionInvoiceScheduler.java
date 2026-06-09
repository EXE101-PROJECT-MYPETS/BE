package com.exe101.commission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommissionInvoiceScheduler {

    private final CommissionInvoiceService commissionInvoiceService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runDailyCommissionJobs() {
        int overdueCount = commissionInvoiceService.markOverdueInvoices();
        int generatedCount = commissionInvoiceService.generateInvoicesForClosedPeriodIfNeeded().size();
        log.info(
                "Commission invoice scheduler finished. generatedInvoices={}, overdueInvoices={}",
                generatedCount,
                overdueCount
        );
    }
}
