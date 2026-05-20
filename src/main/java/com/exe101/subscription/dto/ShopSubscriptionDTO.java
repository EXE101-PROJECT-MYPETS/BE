package com.exe101.subscription.dto;

import com.exe101.subscription.entity.ShopSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopSubscriptionDTO {
    private Long id;
    private Long shopId;
    private Long planId;
    private String planType;
    private ShopSubscriptionStatus status;
    private OffsetDateTime startedAt;
    private OffsetDateTime trialEndsAt;
    private OffsetDateTime currentPeriodStart;
    private OffsetDateTime currentPeriodEnd;
    private OffsetDateTime expiredAt;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private SubscriptionPlanDTO plan;
}
