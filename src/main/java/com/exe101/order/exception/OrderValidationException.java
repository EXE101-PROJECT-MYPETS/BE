package com.exe101.order.exception;

import com.exe101.exception.ValidateException;

public class OrderValidationException extends OrderException implements ValidateException {
    public OrderValidationException(String code, String message) {
        super(code, message);
    }
}
