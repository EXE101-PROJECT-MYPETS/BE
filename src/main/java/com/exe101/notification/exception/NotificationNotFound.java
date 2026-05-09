package com.exe101.notification.exception;

import com.exe101.exception.NotFoundException;

public class NotificationNotFound extends NotificationException implements NotFoundException {
    public NotificationNotFound(String code, String message) {
        super(code, message);
    }
}
