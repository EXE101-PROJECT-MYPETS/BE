package com.exe101.booking.exception;

import com.exe101.exception.AppException;

public class BookingException extends AppException {
    protected BookingException(String code, String message) {
        super(code, message);
    }
}
