package com.exe101.resource.exception;

import com.exe101.exception.NotFoundException;

public class ResourceNotFound extends ResourceException implements NotFoundException {
    public ResourceNotFound(String code, String message) {
        super(code, message);
    }
}
