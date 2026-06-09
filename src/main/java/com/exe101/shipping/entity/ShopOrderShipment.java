package com.exe101.shipping.entity;

import com.exe101.order.entity.CustomerOrder;
import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity(name = "ShopOrderShipment")
@Table(name = "shop_order_shipments")
public class ShopOrderShipment {

    public static final String CARRIER_GHTK = "GHTK";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String carrier = CARRIER_GHTK;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "label_id")
    private String labelId;

    @Column(name = "tracking_id")
    private String trackingId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "shipment_status")
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "ghtk_status_id")
    private Integer ghtkStatusId;

    @Column(name = "last_action_time")
    private OffsetDateTime lastActionTime;

    @Column(name = "actual_shipping_fee")
    private Long actualShippingFee;

    private BigDecimal weight;

    @Column(name = "pick_money")
    private Long pickMoney;

    @Column(name = "return_part_package")
    private Integer returnPartPackage;

    @Column(name = "reason_code")
    private String reasonCode;

    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnore
    private CustomerOrder order;
}
