package com.exe101.ai.exception;

import com.exe101.exception.NotFoundException;

public class AiNotFound extends AiException implements NotFoundException {
    public AiNotFound(String code, String message) {
        super(code, message);
    }
}
