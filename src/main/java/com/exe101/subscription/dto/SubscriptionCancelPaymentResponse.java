package com.exe101.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCancelPaymentResponse {
    private Long paymentId;
    private String invoiceNumber;
    private String status;
    private String message;
}
