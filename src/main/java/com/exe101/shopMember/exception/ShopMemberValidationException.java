package com.exe101.shopMember.exception;

import com.exe101.exception.ValidateException;

public class ShopMemberValidationException extends ShopMemberException implements ValidateException {
    public ShopMemberValidationException(String code, String message) {
        super(code, message);
    }
}
