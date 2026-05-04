package com.exe101.product.exception;

import com.exe101.exception.ValidateException;

public class ProductValidationException extends ProductException implements ValidateException {
    public ProductValidationException(String code, String message) {
        super(code, message);
    }
}
