package com.exe101.shopGhtkConfig.exception;

import com.exe101.exception.ValidateException;

public class ShopGhtkConfigValidationException extends ShopGhtkConfigException implements ValidateException {
    public ShopGhtkConfigValidationException(String code, String message) {
        super(code, message);
    }
}
