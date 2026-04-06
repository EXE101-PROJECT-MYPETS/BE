package com.exe101.service_shop.exception;

import com.exe101.exception.NotFoundException;

public class ServiceNotFound extends ServiceException implements NotFoundException {
    public ServiceNotFound(String code, String message) {
        super(code, message);
    }
}
