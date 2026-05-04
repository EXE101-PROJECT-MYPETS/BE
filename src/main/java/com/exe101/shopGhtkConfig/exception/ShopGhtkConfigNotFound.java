package com.exe101.shopGhtkConfig.exception;

import com.exe101.exception.NotFoundException;

public class ShopGhtkConfigNotFound extends ShopGhtkConfigException implements NotFoundException {
    public ShopGhtkConfigNotFound(String code, String message) {
        super(code, message);
    }
}
