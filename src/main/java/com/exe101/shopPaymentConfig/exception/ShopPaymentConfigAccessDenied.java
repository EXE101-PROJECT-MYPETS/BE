package com.exe101.shopPaymentConfig.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class ShopPaymentConfigAccessDenied extends ShopPaymentConfigException implements PermissionNotAllowedException {
    public ShopPaymentConfigAccessDenied(String code, String message) {
        super(code, message);
    }
}
