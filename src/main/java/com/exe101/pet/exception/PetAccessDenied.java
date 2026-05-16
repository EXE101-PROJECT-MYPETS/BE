package com.exe101.pet.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class PetAccessDenied extends PetException implements PermissionNotAllowedException {
    public PetAccessDenied(String code, String message) {
        super(code, message);
    }
}
