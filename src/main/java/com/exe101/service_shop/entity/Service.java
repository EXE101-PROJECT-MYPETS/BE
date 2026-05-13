package com.exe101.service_shop.entity;

import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "services",
        uniqueConstraints = {@UniqueConstraint(name = "uq_services_shop_name", columnNames = {"shop_id", "name"})},
        indexes = {
                @Index(name = "idx_services_shop_active", columnList = "shop_id, active"),
                @Index(name = "idx_services_shop_category_active", columnList = "shop_id, category_id, active")
        }
)
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    @Column(name = "base_price", nullable = false)
    private Long basePrice;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "image_url")
    private String imageUrl;

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
    private ServiceCategory category;
}
