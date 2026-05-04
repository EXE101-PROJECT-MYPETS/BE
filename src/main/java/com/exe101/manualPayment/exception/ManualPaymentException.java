package com.exe101.manualPayment.exception;

import com.exe101.exception.AppException;

public class ManualPaymentException extends AppException {
    protected ManualPaymentException(String code, String message) {
        super(code, message);
    }
}
