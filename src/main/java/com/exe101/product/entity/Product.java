package com.exe101.product.entity;

import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_products_shop_sku", columnNames = {"shop_id", "sku"})
        },
        indexes = {
                @Index(name = "idx_products_shop_active", columnList = "shop_id, active"),
                @Index(name = "idx_products_shop_category_active", columnList = "shop_id, category_id, active")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_id")
    private Long categoryId;

    private String unit;

    @Column(nullable = false)
    private Long price = 0L;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightKg = new BigDecimal("100");

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "category_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private ProductCategory category;
}
