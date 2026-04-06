package com.exe101.inventory.entity;

import com.exe101.product.entity.Product;
import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "qty_delta", nullable = false)
    private Long qtyDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryReason reason;

    @Column(name = "ref_type")
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private Product product;
}
