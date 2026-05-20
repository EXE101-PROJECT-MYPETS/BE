package com.exe101.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "opening_hours", length = 20)
    private String openingHours;

    @Column(name = "closing_hours", length = 20)
    private String closingHours;

    @Column(name = "facebook_url", columnDefinition = "text")
    private String facebookUrl;

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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "shop_status")
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
