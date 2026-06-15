package com.exe101.review.exception;

import com.exe101.exception.ValidateException;

public class ReviewValidationException extends ReviewException implements ValidateException {
    public ReviewValidationException(String code, String message) {
        super(code, message);
    }
}
