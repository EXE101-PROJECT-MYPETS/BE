package com.exe101.inventory.exception;

import com.exe101.exception.AppException;

public class InventoryException extends AppException {
    protected InventoryException(String code, String message) {
        super(code, message);
    }
}
