package com.exe101.servicePackage.exception;

import com.exe101.exception.NotFoundException;

public class ServicePackageNotFound extends ServicePackageException implements NotFoundException {
    public ServicePackageNotFound(String code, String message) {
        super(code, message);
    }
}
