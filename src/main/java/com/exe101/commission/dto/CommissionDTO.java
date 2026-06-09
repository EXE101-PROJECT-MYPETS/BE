package com.exe101.commission.dto;

import com.exe101.commission.entity.CommissionSourceType;
import com.exe101.commission.entity.CommissionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionDTO {
    private Long id;
    private Long shopId;
    private CommissionSourceType sourceType;
    private Long sourceId;
    private String sourceCode;
    private Long invoiceId;
    private String invoiceCode;
    private OffsetDateTime completedAt;
    private Long grossAmount;
    private Long discountAmount;
    private Long shippingFee;
    private Long commissionBase;
    private Integer commissionRateBps;
    private Long commissionAmount;
    private CommissionStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime invoicedAt;
    private OffsetDateTime collectedAt;
    private OffsetDateTime refundedAt;
}
