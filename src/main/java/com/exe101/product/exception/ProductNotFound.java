package com.exe101.product.exception;

import com.exe101.exception.NotFoundException;

public class ProductNotFound extends ProductException implements NotFoundException {
    public ProductNotFound(String code, String message) {
        super(code, message);
    }
}
