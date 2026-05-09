package com.exe101.review.exception;

import com.exe101.exception.DuplicateException;

public class ReviewDuplicate extends ReviewException implements DuplicateException {
    public ReviewDuplicate(String code, String message) {
        super(code, message);
    }
}
