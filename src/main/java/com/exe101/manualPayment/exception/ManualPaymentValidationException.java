package com.exe101.manualPayment.exception;

import com.exe101.exception.ValidateException;

public class ManualPaymentValidationException extends ManualPaymentException implements ValidateException {
    public ManualPaymentValidationException(String code, String message) {
        super(code, message);
    }
}
