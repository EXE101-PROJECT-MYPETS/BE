package com.exe101.servicePackage.entity;

import com.exe101.booking.entity.Booking;
import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "package_ledger")
public class PackageLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "customer_package_id", nullable = false)
    private Long customerPackageId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "delta_uses", nullable = false)
    private Integer deltaUses;

    @Column(name = "delta_amount", nullable = false)
    private Long deltaAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageLedgerReason reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_package_id", insertable = false, updatable = false)
    @JsonIgnore
    private CustomerPackage customerPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", insertable = false, updatable = false)
    @JsonIgnore
    private Booking booking;
}
