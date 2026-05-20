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
public class SubscriptionPaymentStatusResponse {
    private Long paymentId;
    private String invoiceNumber;
    private String status;
    private OffsetDateTime paidAt;
    private OffsetDateTime expiredAt;
    private OffsetDateTime subscriptionExpiredAt;
}
