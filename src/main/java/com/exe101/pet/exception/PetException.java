package com.exe101.pet.exception;

import com.exe101.exception.AppException;

public class PetException extends AppException {
    protected PetException(String code, String message) {
        super(code, message);
    }
}
