package com.exe101.shipping.entity;

public enum ShipmentStatus {
    CREATED,
    PENDING_PICKUP,
    ACCEPTED,
    PICKED_UP,
    DELIVERING,
    DELIVERED,
    RECONCILED,
    CANCELED,
    PICKUP_FAILED,
    PICKUP_DELAYED,
    DELIVERY_FAILED,
    DELIVERY_DELAYED,
    RETURNING,
    RETURNED
}
