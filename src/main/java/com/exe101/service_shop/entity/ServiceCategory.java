package com.exe101.service_shop.entity;

import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "service_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_service_categories_shop_name", columnNames = {"shop_id", "name"}),
                @UniqueConstraint(name = "uq_service_categories_shop_id", columnNames = {"shop_id", "id"})
        },
        indexes = {
                @Index(name = "idx_service_categories_shop_active", columnList = "shop_id, active"),
                @Index(name = "idx_service_categories_shop_sort_order", columnList = "shop_id, sort_order")
        }
)
public class ServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;
}
