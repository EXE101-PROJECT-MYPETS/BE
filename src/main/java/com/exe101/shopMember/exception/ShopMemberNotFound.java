package com.exe101.shopMember.exception;

import com.exe101.exception.NotFoundException;

public class ShopMemberNotFound extends ShopMemberException implements NotFoundException {
    public ShopMemberNotFound(String code, String message) {
        super(code, message);
    }
}
