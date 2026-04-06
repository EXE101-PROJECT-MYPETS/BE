package com.exe101.vaccine.exception;

import com.exe101.exception.NotFoundException;

public class VaccineNotFound extends VaccineException implements NotFoundException {
    public VaccineNotFound(String code, String message) {
        super(code, message);
    }
}
