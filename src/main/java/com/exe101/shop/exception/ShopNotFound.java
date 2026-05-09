package com.exe101.shop.exception;

import com.exe101.exception.NotFoundException;

public class ShopNotFound extends ShopException implements NotFoundException {
    public ShopNotFound(String code, String message) {
        super(code, message);
    }
}
