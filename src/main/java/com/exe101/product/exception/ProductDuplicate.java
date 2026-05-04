package com.exe101.product.exception;

import com.exe101.exception.DuplicateException;

public class ProductDuplicate extends ProductException implements DuplicateException {
    public ProductDuplicate(String code, String message) {
        super(code, message);
    }
}
