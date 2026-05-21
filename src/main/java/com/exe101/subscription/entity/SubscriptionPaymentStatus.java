package com.exe101.subscription.entity;

public enum SubscriptionPaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELED,
    PAID_AFTER_CANCEL,
    EXPIRED,
    SUCCESS_LATE
}
