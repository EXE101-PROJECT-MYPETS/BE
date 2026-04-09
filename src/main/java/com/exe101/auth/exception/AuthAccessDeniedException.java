package com.exe101.auth.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class AuthAccessDeniedException extends AuthException implements PermissionNotAllowedException {
    public AuthAccessDeniedException(String code, String message) {
        super(code, message);
    }
}
