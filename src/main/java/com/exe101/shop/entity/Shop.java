package com.exe101.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "address_text")
    private String addressText;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", nullable = false)
    private LocationSource locationSource = LocationSource.MANUAL;

    @Column(name = "location_accuracy_m")
    private Integer locationAccuracyM;

    @Column(name = "location_updated_at", nullable = false)
    private OffsetDateTime locationUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShopStatus status = ShopStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    /**
     * DB đã có DEFAULT now() cho created_at/location_updated_at
     * Nhưng để tránh insert null (nhất là khi Hibernate include column),
     * ta set fallback trước khi persist.
     */
    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        if (locationUpdatedAt == null) {
            locationUpdatedAt = now;
        }
        if (status == null) {
            status = ShopStatus.ACTIVE;
        }
        if (locationSource == null) {
            locationSource = LocationSource.MANUAL;
        }
    }
}
