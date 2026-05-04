package com.exe101.product.exception;

import com.exe101.exception.NotFoundException;

public class ProductCategoryNotFound extends ProductException implements NotFoundException {
    public ProductCategoryNotFound(String code, String message) {
        super(code, message);
    }
}
