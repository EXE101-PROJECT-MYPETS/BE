package com.exe101.order.exception;

import com.exe101.exception.NotFoundException;

public class OrderNotFound extends OrderException implements NotFoundException {
    public OrderNotFound(String code, String message) {
        super(code, message);
    }
}
