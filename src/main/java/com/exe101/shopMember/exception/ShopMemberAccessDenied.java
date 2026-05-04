package com.exe101.shopMember.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class ShopMemberAccessDenied extends ShopMemberException implements PermissionNotAllowedException {
    public ShopMemberAccessDenied(String code, String message) {
        super(code, message);
    }
}
