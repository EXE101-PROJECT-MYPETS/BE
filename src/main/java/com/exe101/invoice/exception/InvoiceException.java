package com.exe101.invoice.exception;

import com.exe101.exception.AppException;

public class InvoiceException extends AppException {
    protected InvoiceException(String code, String message) {
        super(code, message);
    }
}
