package com.exe101.commission.dto;

import com.exe101.commission.entity.CommissionSourceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionInvoiceItemDTO {
    private Long id;
    private Long invoiceId;
    private Long commissionId;
    private CommissionSourceType sourceType;
    private Long sourceId;
    private String sourceCode;
    private OffsetDateTime completedAt;
    private Long commissionBase;
    private Long commissionAmount;
    private OffsetDateTime createdAt;
}
