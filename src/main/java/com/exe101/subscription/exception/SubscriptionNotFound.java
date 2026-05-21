package com.exe101.subscription.exception;

import com.exe101.exception.NotFoundException;

public class SubscriptionNotFound extends SubscriptionException implements NotFoundException {
    public SubscriptionNotFound(String code, String message) {
        super(code, message);
    }
}
