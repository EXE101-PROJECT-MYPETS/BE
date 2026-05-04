package com.exe101.customerAddress.exception;

import com.exe101.exception.NotFoundException;

public class CustomerAddressNotFound extends CustomerAddressException implements NotFoundException {
    public CustomerAddressNotFound(String code, String message) {
        super(code, message);
    }
}
