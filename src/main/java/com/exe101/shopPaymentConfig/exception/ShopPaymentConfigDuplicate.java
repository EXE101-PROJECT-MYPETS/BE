package com.exe101.shopPaymentConfig.exception;

import com.exe101.exception.DuplicateException;

public class ShopPaymentConfigDuplicate extends ShopPaymentConfigException implements DuplicateException {
    public ShopPaymentConfigDuplicate(String code, String message) {
        super(code, message);
    }
}
