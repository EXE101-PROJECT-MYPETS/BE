package com.exe101.user.exception;

import com.exe101.exception.AppException;
import com.exe101.exception.DuplicateException;

public class UserDuplicate extends AppException implements DuplicateException {
    public UserDuplicate(String code, String message) {
        super(code, message);
    }
}
