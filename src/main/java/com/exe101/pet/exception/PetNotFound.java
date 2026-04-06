package com.exe101.pet.exception;

import com.exe101.exception.NotFoundException;

public class PetNotFound extends PetException implements NotFoundException {
    public PetNotFound(String code, String message) {
        super(code, message);
    }
}
