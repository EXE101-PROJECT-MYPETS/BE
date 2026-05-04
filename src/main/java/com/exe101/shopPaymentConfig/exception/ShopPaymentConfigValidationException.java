package com.exe101.shopPaymentConfig.exception;

import com.exe101.exception.ValidateException;

public class ShopPaymentConfigValidationException extends ShopPaymentConfigException implements ValidateException {
    public ShopPaymentConfigValidationException(String code, String message) {
        super(code, message);
    }
}
