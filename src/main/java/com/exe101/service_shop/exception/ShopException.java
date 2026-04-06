package com.exe101.service_shop.exception;

import com.exe101.exception.AppException;

public class ShopException extends AppException {
    protected ShopException(String code, String message) {
        super(code, message);
    }
}
