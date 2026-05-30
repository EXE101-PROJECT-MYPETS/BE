package com.exe101.shipping.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity(name = "ShippingWebhookLog")
@Table(name = "shipping_webhook_logs")
public class ShippingWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String carrier = ShopOrderShipment.CARRIER_GHTK;

    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "partner_id")
    private String partnerId;

    @Column(name = "label_id")
    private String labelId;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "action_time")
    private OffsetDateTime actionTime;

    private String hash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode rawPayloadJson;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "processing_status", nullable = false, columnDefinition = "shipping_webhook_processing_status")
    private ShippingWebhookProcessingStatus processingStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", insertable = false, updatable = false)
    private ShopOrderShipment shipment;
}
