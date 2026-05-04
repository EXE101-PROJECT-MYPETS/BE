package com.exe101.product.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class ProductAccessDenied extends ProductException implements PermissionNotAllowedException {
    public ProductAccessDenied(String code, String message) {
        super(code, message);
    }
}
