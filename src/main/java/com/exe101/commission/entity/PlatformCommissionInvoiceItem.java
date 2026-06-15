package com.exe101.commission.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "platform_commission_invoice_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_platform_commission_invoice_items_commission",
                columnNames = "commission_id"
        )
)
public class PlatformCommissionInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "commission_id", nullable = false)
    private Long commissionId;

    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount = 0L;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", insertable = false, updatable = false)
    @JsonIgnore
    private PlatformCommissionInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id", insertable = false, updatable = false)
    @JsonIgnore
    private PlatformCommission commission;
}
