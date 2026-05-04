package com.exe101.ghtk.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class GhtkAccessDenied extends GhtkException implements PermissionNotAllowedException {
    public GhtkAccessDenied(String code, String message) {
        super(code, message);
    }
}
