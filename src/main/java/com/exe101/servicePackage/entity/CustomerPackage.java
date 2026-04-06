package com.exe101.servicePackage.entity;

import com.exe101.customer.entity.Customer;
import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_packages")
public class CustomerPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "purchased_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime purchasedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "package_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private ServicePackage servicePackage;
}
