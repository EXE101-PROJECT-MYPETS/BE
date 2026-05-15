package com.exe101.ai.exception;

import com.exe101.exception.AppException;

public class AiException extends AppException {
    protected AiException(String code, String message) {
        super(code, message);
    }
}
