package com.exe101.shopPaymentConfig.exception;

import com.exe101.exception.NotFoundException;

public class ShopPaymentConfigNotFound extends ShopPaymentConfigException implements NotFoundException {
    public ShopPaymentConfigNotFound(String code, String message) {
        super(code, message);
    }
}
