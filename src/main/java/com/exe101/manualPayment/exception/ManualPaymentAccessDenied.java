package com.exe101.manualPayment.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class ManualPaymentAccessDenied extends ManualPaymentException implements PermissionNotAllowedException {
    public ManualPaymentAccessDenied(String code, String message) {
        super(code, message);
    }
}
