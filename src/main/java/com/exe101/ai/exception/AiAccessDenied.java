package com.exe101.ai.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class AiAccessDenied extends AiException implements PermissionNotAllowedException {
    public AiAccessDenied(String code, String message) {
        super(code, message);
    }
}
