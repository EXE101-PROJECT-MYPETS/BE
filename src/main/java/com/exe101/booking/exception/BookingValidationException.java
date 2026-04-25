package com.exe101.booking.exception;

import com.exe101.exception.ValidateException;

public class BookingValidationException extends BookingException implements ValidateException {
    public BookingValidationException(String code, String message) {
        super(code, message);
    }
}
