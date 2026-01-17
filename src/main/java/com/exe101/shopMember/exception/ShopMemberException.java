package com.exe101.shopMember.exception;

import com.exe101.exception.AppException;

public class ShopMemberException extends AppException {
    protected ShopMemberException(String code, String message) {
        super(code, message);
    }
}
