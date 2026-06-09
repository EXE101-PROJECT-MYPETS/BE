package com.exe101.shipping.entity;

public enum ShippingWebhookProcessingStatus {
    RECEIVED,
    UNKNOWN_SHIPMENT,
    STALE_OR_DUPLICATE,
    SHIPPER_EVENT,
    APPLIED,
    FAILED
}
