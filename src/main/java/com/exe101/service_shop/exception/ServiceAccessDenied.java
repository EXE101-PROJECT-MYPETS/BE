package com.exe101.service_shop.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class ServiceAccessDenied extends ServiceException implements PermissionNotAllowedException {
    public ServiceAccessDenied(String code, String message) {
        super(code, message);
    }
}
