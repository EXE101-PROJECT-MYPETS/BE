package com.exe101.resource.exception;

import com.exe101.exception.AppException;

public class ResourceException extends AppException {
    protected ResourceException(String code, String message) {
        super(code, message);
    }
}
