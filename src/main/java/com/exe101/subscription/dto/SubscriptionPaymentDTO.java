package com.exe101.subscription.dto;

import com.exe101.subscription.entity.SubscriptionPaymentMethod;
import com.exe101.subscription.entity.SubscriptionPaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPaymentDTO {
    private Long id;
    private Long shopId;
    private Long subscriptionId;
    private Long planId;
    private String planCode;
    private Long amount;
    private Integer durationMonths;
    private Integer durationDays;
    private String invoiceNumber;
    private String provider;
    private String transferContent;
    private SubscriptionPaymentStatus status;
    private SubscriptionPaymentMethod paymentMethod;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
    private OffsetDateTime paidAt;
    private String providerTransactionId;
    private OffsetDateTime expiredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private SubscriptionPlanDTO plan;
}
