package com.exe101.order.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PACKING,
    WAITING_GHTK_PICKUP,
    GHTK_PICKED_UP,
    SHIPPING,
    COMPLETED,
    CANCELLED
}
