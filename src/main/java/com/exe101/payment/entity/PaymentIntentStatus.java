package com.exe101.payment.entity;

public enum PaymentIntentStatus {
    REQUIRES_PAYMENT_METHOD,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
