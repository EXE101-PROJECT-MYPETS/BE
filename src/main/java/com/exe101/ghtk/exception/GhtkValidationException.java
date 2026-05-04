package com.exe101.ghtk.exception;

import com.exe101.exception.ValidateException;

public class GhtkValidationException extends GhtkException implements ValidateException {
    public GhtkValidationException(String code, String message) {
        super(code, message);
    }
}
