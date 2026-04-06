package com.exe101.service_shop.exception;

import com.exe101.exception.AppException;

public class ServiceException extends AppException {
    protected ServiceException(String code, String message) {
        super(code, message);
    }
}
