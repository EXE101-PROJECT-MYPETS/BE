package com.exe101.exception;

import lombok.Getter;

@Getter
public abstract class AppException extends RuntimeException {

    private final String code;
    private final Object data;

    protected AppException(String code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    protected AppException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

}