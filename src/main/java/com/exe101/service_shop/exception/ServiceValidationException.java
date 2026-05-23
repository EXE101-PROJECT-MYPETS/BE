package com.exe101.service_shop.exception;

import com.exe101.exception.ValidateException;

public class ServiceValidationException extends ServiceException implements ValidateException {
    public ServiceValidationException(String code, String message) {
        super(code, message);
    }
}
