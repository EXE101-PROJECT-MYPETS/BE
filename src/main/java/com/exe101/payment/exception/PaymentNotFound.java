package com.exe101.payment.exception;

import com.exe101.exception.NotFoundException;

public class PaymentNotFound extends PaymentException implements NotFoundException {
    public PaymentNotFound(String code, String message) {
        super(code, message);
    }
}
