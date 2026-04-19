package com.exe101.service_shop.exception;

import com.exe101.exception.DuplicateException;

public class ServiceDuplicate extends ServiceException implements DuplicateException {
    public ServiceDuplicate(String code, String message) {
        super(code, message);
    }
}
