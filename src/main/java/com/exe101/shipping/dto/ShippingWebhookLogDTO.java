package com.exe101.shipping.dto;

import com.exe101.shipping.entity.ShippingWebhookProcessingStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingWebhookLogDTO {
    private Long id;
    private String carrier;
    private Long shipmentId;
    private Long shopId;
    private Long orderId;
    private String partnerId;
    private String labelId;
    private Integer statusId;
    private OffsetDateTime actionTime;
    private ShippingWebhookProcessingStatus processingStatus;
    private String errorMessage;
    private JsonNode rawPayloadJson;
    private OffsetDateTime createdAt;
}
