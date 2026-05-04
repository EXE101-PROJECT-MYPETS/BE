package com.exe101.invoice.exception;

import com.exe101.exception.ValidateException;

public class InvoiceValidationException extends InvoiceException implements ValidateException {
    public InvoiceValidationException(String code, String message) {
        super(code, message);
    }
}
