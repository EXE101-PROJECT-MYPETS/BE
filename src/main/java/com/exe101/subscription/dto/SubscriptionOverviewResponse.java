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
public class SubscriptionOverviewResponse {
    private Long shopId;
    private String planType;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime expiredAt;
    private long remainingDays;
    private Integer trialTotalDays;
    private long usedDays;
    private long planTotalDays;
    private OffsetDateTime subscriptionStartedAt;
    private OffsetDateTime trialEndsAt;
    private OffsetDateTime currentPeriodStart;
    private OffsetDateTime currentPeriodEnd;
    private long currentPeriodRemainingDays;
    private Long monthlyPrice;
    private String currency;
    private boolean canRenew;
    private String message;
}
