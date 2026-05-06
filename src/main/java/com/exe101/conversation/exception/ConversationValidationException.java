package com.exe101.conversation.exception;

import com.exe101.exception.ValidateException;

public class ConversationValidationException extends ConversationException implements ValidateException {
    public ConversationValidationException(String code, String message) {
        super(code, message);
    }
}
