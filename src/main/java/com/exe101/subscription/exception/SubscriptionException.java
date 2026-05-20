package com.exe101.subscription.exception;

import com.exe101.exception.AppException;

public class SubscriptionException extends AppException {
    protected SubscriptionException(String code, String message) {
        super(code, message);
    }
}
