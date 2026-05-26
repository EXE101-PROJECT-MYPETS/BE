package com.exe101.userAddress.exception;

import com.exe101.exception.NotFoundException;

public class UserAddressNotFound extends UserAddressException implements NotFoundException {
    public UserAddressNotFound(String code, String message) {
        super(code, message);
    }
}
