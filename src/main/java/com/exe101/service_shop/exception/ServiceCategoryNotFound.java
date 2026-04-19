package com.exe101.service_shop.exception;

import com.exe101.exception.NotFoundException;

public class ServiceCategoryNotFound extends ServiceException implements NotFoundException {
    public ServiceCategoryNotFound(String code, String message) {
        super(code, message);
    }
}
