package com.exe101.commission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionPaymentInfoDTO {
    private Long invoiceId;
    private String invoiceCode;
    private Long amount;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrUrl;
    private OffsetDateTime dueAt;
}
