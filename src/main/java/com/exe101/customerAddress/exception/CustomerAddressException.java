package com.exe101.customerAddress.exception;

import com.exe101.exception.AppException;

public class CustomerAddressException extends AppException {
    protected CustomerAddressException(String code, String message) {
        super(code, message);
    }
}
