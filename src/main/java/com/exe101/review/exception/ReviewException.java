package com.exe101.review.exception;

import com.exe101.exception.AppException;

public class ReviewException extends AppException {
    protected ReviewException(String code, String message) {
        super(code, message);
    }
}
