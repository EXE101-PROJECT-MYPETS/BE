package com.exe101.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSummaryDTO {
    private boolean hasUnpaidInvoice;
    private Long unpaidInvoiceAmount;
    private Long unpaidInvoiceCount;
    private Long pendingCommissionAmount;
    private Long pendingCommissionCount;
    private Long paidInvoiceAmount;
    private Long paidInvoiceCount;
    private Long overdueInvoiceAmount;
    private Long overdueInvoiceCount;
    private Long nearestUnpaidInvoiceId;
    private LocalDate currentPeriodFrom;
    private LocalDate currentPeriodTo;
    private LocalDate nextInvoiceDate;
    private OffsetDateTime nextDueDate;
    private String message;
}
