package com.exe101.invoice.exception;

import com.exe101.exception.NotFoundException;

public class InvoiceNotFound extends InvoiceException implements NotFoundException {
    public InvoiceNotFound(String code, String message) {
        super(code, message);
    }
}
