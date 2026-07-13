package com.exe101.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommissionMonthlySummaryDTO {
    private Long shopCount;
    private Long transactionCount;
    private Long outstandingShopCount;
    private Long overdueShopCount;
    private Long grossAmount;
    private Long commissionBase;
    private Long commissionAmount;
    private Long pendingAmount;
    private Long invoicedAmount;
    private Long collectedAmount;
    private Long outstandingAmount;
    private Long overdueAmount;
}
