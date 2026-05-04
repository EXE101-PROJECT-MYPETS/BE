package com.exe101.customerAddress.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class CustomerAddressAccessDenied extends CustomerAddressException implements PermissionNotAllowedException {
    public CustomerAddressAccessDenied(String code, String message) {
        super(code, message);
    }
}
