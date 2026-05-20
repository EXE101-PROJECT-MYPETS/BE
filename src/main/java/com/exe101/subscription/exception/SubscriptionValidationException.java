package com.exe101.subscription.exception;

import com.exe101.exception.ValidateException;

public class SubscriptionValidationException extends SubscriptionException implements ValidateException {
    public SubscriptionValidationException(String code, String message) {
        super(code, message);
    }
}
