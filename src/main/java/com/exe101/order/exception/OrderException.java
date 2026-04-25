package com.exe101.order.exception;

import com.exe101.exception.AppException;

public class OrderException extends AppException {
    protected OrderException(String code, String message) {
        super(code, message);
    }
}
