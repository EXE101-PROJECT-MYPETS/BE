package com.exe101.email.exception;

public class EmailSendException extends EmailException {
    public EmailSendException(String code, String message) {
        super(code, message);
    }
}
