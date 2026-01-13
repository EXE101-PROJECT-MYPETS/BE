package com.exe101.user.exception;

import com.exe101.exception.NotFoundException;

public class UserNotFound extends UserException implements NotFoundException {
    public UserNotFound(String code, String message) {
        super(code, message);
    }
}
