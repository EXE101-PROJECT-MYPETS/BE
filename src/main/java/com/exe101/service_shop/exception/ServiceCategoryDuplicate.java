package com.exe101.service_shop.exception;

import com.exe101.exception.DuplicateException;

public class ServiceCategoryDuplicate extends ServiceException implements DuplicateException {
    public ServiceCategoryDuplicate(String code, String message) {
        super(code, message);
    }
}
