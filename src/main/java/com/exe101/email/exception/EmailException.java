package com.exe101.email.exception;

import com.exe101.exception.AppException;

public class EmailException extends AppException {
    protected EmailException(String code, String message) {
        super(code, message);
    }
}
