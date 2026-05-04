package com.exe101.shopMember.exception;

import com.exe101.exception.DuplicateException;

public class ShopMemberDuplicate extends ShopMemberException implements DuplicateException {
    public ShopMemberDuplicate(String code, String message) {
        super(code, message);
    }
}
