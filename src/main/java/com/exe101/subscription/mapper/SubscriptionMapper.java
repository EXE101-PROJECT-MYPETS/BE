package com.exe101.subscription.mapper;

import com.exe101.subscription.dto.ShopSubscriptionDTO;
import com.exe101.subscription.dto.SubscriptionPaymentDTO;
import com.exe101.subscription.dto.SubscriptionPlanDTO;
import com.exe101.subscription.entity.ShopSubscription;
import com.exe101.subscription.entity.SubscriptionPayment;
import com.exe101.subscription.entity.SubscriptionPlan;

public class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionPlanDTO toPlanDTO(SubscriptionPlan entity) {
        if (entity == null) {
            return null;
        }
        return new SubscriptionPlanDTO(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDurationMonths(),
                entity.getPrice(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ShopSubscriptionDTO toSubscriptionDTO(ShopSubscription entity) {
        if (entity == null) {
            return null;
        }
        return new ShopSubscriptionDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getPlanId(),
                entity.getPlanType(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getTrialEndsAt(),
                entity.getCurrentPeriodStart(),
                entity.getCurrentPeriodEnd(),
                entity.getExpiredAt(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                toPlanDTO(entity.getPlan())
        );
    }

    public static SubscriptionPaymentDTO toPaymentDTO(SubscriptionPayment entity) {
        if (entity == null) {
            return null;
        }
        return new SubscriptionPaymentDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getSubscriptionId(),
                entity.getPlanId(),
                entity.getPlanCode(),
                entity.getAmount(),
                entity.getDurationMonths(),
                entity.getDurationDays(),
                entity.getInvoiceNumber(),
                entity.getProvider(),
                entity.getTransferContent(),
                entity.getStatus(),
                entity.getPaymentMethod(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getPaidAt(),
                entity.getProviderTransactionId(),
                entity.getExpiredAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                toPlanDTO(entity.getPlan())
        );
    }
}
