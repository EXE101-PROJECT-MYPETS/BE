package com.exe101.conversation.exception;

import com.exe101.exception.AppException;

public class ConversationException extends AppException {
    protected ConversationException(String code, String message) {
        super(code, message);
    }
}
