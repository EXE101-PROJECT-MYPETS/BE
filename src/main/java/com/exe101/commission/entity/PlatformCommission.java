package com.exe101.commission.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "platform_commissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_platform_commissions_source",
                columnNames = {"source_type", "source_id"}
        )
)
public class PlatformCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source_type", nullable = false, columnDefinition = "commission_source_type")
    private CommissionSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "gross_amount", nullable = false)
    private Long grossAmount = 0L;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount = 0L;

    @Column(name = "shipping_fee", nullable = false)
    private Long shippingFee = 0L;

    @Column(name = "commission_base", nullable = false)
    private Long commissionBase = 0L;

    @Column(name = "commission_rate_bps", nullable = false)
    private Integer commissionRateBps = 1500;

    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount = 0L;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "commission_status")
    private CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "invoiced_at")
    private OffsetDateTime invoicedAt;

    @Column(name = "collected_at")
    private OffsetDateTime collectedAt;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private com.exe101.shop.entity.Shop shop;
}
