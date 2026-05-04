package com.exe101.shopGhtkConfig.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class ShopGhtkConfigAccessDenied extends ShopGhtkConfigException implements PermissionNotAllowedException {
    public ShopGhtkConfigAccessDenied(String code, String message) {
        super(code, message);
    }
}
