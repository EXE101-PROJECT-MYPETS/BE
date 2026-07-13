package com.exe101.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminShopMonthlyCommissionDTO {
    private Long shopId;
    private String shopName;
    private String shopImageUrl;
    private Long transactionCount;
    private Long invoiceCount;
    private Long grossAmount;
    private Long commissionBase;
    private Long commissionAmount;
    private Long pendingAmount;
    private Long invoicedAmount;
    private Long collectedAmount;
    private Long outstandingAmount;
    private Long overdueAmount;
    private AdminCommissionCollectionStatus status;
}
