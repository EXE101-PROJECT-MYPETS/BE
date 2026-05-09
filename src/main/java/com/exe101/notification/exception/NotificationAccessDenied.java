package com.exe101.notification.exception;

import com.exe101.exception.PermissionNotAllowedException;

public class NotificationAccessDenied extends NotificationException implements PermissionNotAllowedException {
    public NotificationAccessDenied(String code, String message) {
        super(code, message);
    }
}
