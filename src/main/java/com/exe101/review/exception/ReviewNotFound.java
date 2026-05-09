package com.exe101.review.exception;

import com.exe101.exception.NotFoundException;

public class ReviewNotFound extends ReviewException implements NotFoundException {
    public ReviewNotFound(String code, String message) {
        super(code, message);
    }
}
