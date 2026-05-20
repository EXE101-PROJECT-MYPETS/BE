package com.exe101.subscription.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class SubscriptionAccessDenied extends SubscriptionException implements PermissionNotAllowedException {
    public SubscriptionAccessDenied(String code, String message) {
        super(code, message);
    }
}
