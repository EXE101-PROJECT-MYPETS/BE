package com.exe101.shop.exception;

import com.exe101.exception.AppException;

public class ShopException extends AppException {
    protected ShopException(String code, String message) {
        super(code, message);
    }
}
