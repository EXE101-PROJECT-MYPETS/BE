package com.exe101.inventory.exception;

import com.exe101.exception.NotFoundException;

public class InventoryNotFound extends InventoryException implements NotFoundException {
    public InventoryNotFound(String code, String message) {
        super(code, message);
    }
}
