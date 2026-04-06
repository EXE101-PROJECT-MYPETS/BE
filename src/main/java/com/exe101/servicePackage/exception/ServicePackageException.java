package com.exe101.servicePackage.exception;

import com.exe101.exception.AppException;

public class ServicePackageException extends AppException {
    protected ServicePackageException(String code, String message) {
        super(code, message);
    }
}
