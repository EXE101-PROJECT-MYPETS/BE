package com.exe101.vaccine.exception;

import com.exe101.exception.AppException;

public class VaccineException extends AppException {
    protected VaccineException(String code, String message) {
        super(code, message);
    }
}
