package com.exe101.customer.exception;

import com.exe101.exception.AppException;

public class CustomerException extends AppException {
    protected CustomerException(String code, String message) {
        super(code, message);
    }
}
