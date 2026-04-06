package com.exe101.booking.exception;

import com.exe101.exception.NotFoundException;

public class BookingNotFound extends BookingException implements NotFoundException {
    public BookingNotFound(String code, String message) {
        super(code, message);
    }
}
