package com.exe101.conversation.exception;

import com.exe101.exception.NotFoundException;

public class ConversationNotFound extends ConversationException implements NotFoundException {
    public ConversationNotFound(String code, String message) {
        super(code, message);
    }
}
