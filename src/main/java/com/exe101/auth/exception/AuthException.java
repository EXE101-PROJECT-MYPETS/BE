package com.exe101.auth.exception;

import com.exe101.exception.AppException;

public class AuthException extends AppException {
    protected AuthException(String code, String message) {
        super(code, message);
    }
}
