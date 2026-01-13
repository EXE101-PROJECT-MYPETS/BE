package com.exe101.user.exception;

import com.exe101.exception.AppException;

public class UserException extends AppException {
    protected UserException(String code, String message) {
        super(code, message);
    }
}
