package com.exe101.customer.exception;

import com.exe101.exception.NotFoundException;

public class CustomerNotFound extends CustomerException implements NotFoundException {
    public CustomerNotFound(String code, String message) {
        super(code, message);
    }
}
