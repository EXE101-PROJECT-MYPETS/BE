package com.exe101.commission.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "platform_commission_invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_platform_commission_invoices_code", columnNames = "invoice_code"),
                @UniqueConstraint(
                        name = "uq_platform_commission_invoices_shop_period",
                        columnNames = {"shop_id", "period_from", "period_to"}
                )
        }
)
public class PlatformCommissionInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "invoice_code", nullable = false, length = 50)
    private String invoiceCode;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "total_gross_amount", nullable = false)
    private Long totalGrossAmount = 0L;

    @Column(name = "total_commission_amount", nullable = false)
    private Long totalCommissionAmount = 0L;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "commission_invoice_status")
    private CommissionInvoiceStatus status = CommissionInvoiceStatus.PENDING;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "transfer_content", nullable = false, length = 100)
    private String transferContent;

    @Column(name = "qr_url")
    private String qrUrl;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private com.exe101.shop.entity.Shop shop;
}
