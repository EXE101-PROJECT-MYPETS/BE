package com.exe101.notification.exception;

import com.exe101.exception.AppException;

public class NotificationException extends AppException {
    protected NotificationException(String code, String message) {
        super(code, message);
    }
}
