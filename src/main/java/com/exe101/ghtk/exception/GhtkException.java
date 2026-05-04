package com.exe101.ghtk.exception;

import com.exe101.exception.AppException;

public class GhtkException extends AppException {
    protected GhtkException(String code, String message) {
        super(code, message);
    }
}
