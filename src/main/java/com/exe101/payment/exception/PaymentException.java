package com.exe101.payment.exception;

import com.exe101.exception.AppException;

public class PaymentException extends AppException {
    protected PaymentException(String code, String message) {
        super(code, message);
    }
}
