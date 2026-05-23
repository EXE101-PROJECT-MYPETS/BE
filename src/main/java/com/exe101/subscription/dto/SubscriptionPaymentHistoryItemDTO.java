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
public class SubscriptionPaymentHistoryItemDTO {
    private Long paymentId;
    private String invoiceNumber;
    private String planName;
    private Integer months;
    private Long amount;
    private String status;
    private String provider;
    private OffsetDateTime paidAt;
    private OffsetDateTime subscriptionExpiredAt;
    private OffsetDateTime createdAt;
}
