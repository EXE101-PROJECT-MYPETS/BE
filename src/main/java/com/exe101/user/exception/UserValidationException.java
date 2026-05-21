package com.exe101.user.exception;

import com.exe101.exception.ValidateException;

public class UserValidationException extends UserException implements ValidateException {
    public UserValidationException(String code, String message) {
        super(code, message);
    }
}
