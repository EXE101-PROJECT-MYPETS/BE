package com.exe101.product.exception;

import com.exe101.exception.AppException;

public class ProductException extends AppException {
    protected ProductException(String code, String message) {
        super(code, message);
    }
}
