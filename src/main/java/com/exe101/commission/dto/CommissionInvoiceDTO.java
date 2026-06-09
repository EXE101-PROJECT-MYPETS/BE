package com.exe101.commission.dto;

import com.exe101.commission.entity.CommissionInvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionInvoiceDTO {
    private Long id;
    private Long shopId;
    private String invoiceCode;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private Long totalGrossAmount;
    private Long totalCommissionAmount;
    private CommissionInvoiceStatus status;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime dueAt;
    private OffsetDateTime paidAt;
    private List<CommissionInvoiceItemDTO> items;
}
