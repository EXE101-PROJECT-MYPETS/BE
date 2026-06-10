package com.exe101.subscription.dto;

import com.exe101.subscription.entity.SubscriptionPaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionPurchaseRequest {

    @NotNull(message = "Gói subscription không được để trống")
    private Long planId;

    private SubscriptionPaymentMethod paymentMethod = SubscriptionPaymentMethod.BANK_TRANSFER;
}
