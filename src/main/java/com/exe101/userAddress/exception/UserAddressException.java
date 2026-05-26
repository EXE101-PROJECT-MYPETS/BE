package com.exe101.userAddress.exception;

import com.exe101.exception.AppException;

public class UserAddressException extends AppException {
    protected UserAddressException(String code, String message) {
        super(code, message);
    }
}
