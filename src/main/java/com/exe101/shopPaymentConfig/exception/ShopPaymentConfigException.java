package com.exe101.shopPaymentConfig.exception;

import com.exe101.exception.AppException;

public class ShopPaymentConfigException extends AppException {
    protected ShopPaymentConfigException(String code, String message) {
        super(code, message);
    }
}
