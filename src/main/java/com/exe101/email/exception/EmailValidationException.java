package com.exe101.email.exception;

import com.exe101.exception.ValidateException;

public class EmailValidationException extends EmailException implements ValidateException {
    public EmailValidationException(String code, String message) {
        super(code, message);
    }
}
