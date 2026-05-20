package com.exe101.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SepayQrPaymentResponse {
    private Long paymentId;
    private String invoiceNumber;
    private Integer months;
    private Integer durationDays;
    private Long amount;
    private String status;
    private String provider;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrUrl;
    private OffsetDateTime expiredAt;
    private OffsetDateTime subscriptionExpiredAt;
    private OffsetDateTime createdAt;
}
