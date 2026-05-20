package com.exe101.subscription.entity;

import com.exe101.shop.entity.Shop;
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
@Table(name = "subscription_payments")
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "plan_code", nullable = false, length = 50)
    private String planCode = "MONTHLY";

    @Column(nullable = false)
    private Long amount;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths = 1;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays = 30;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(nullable = false, length = 50)
    private String provider = "SEPAY";

    @Column(name = "transfer_content", nullable = false, length = 100)
    private String transferContent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "subscription_payment_status")
    private SubscriptionPaymentStatus status = SubscriptionPaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_method", nullable = false, columnDefinition = "subscription_payment_method")
    private SubscriptionPaymentMethod paymentMethod = SubscriptionPaymentMethod.BANK_TRANSFER;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "expired_at", nullable = false)
    private OffsetDateTime expiredAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", insertable = false, updatable = false)
    @JsonIgnore
    private ShopSubscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    @JsonIgnore
    private SubscriptionPlan plan;
}
