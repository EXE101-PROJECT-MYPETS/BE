package com.exe101.shopGhtkConfig.entity;

import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "shop_ghtk_configs")
public class ShopGhtkConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(name = "encrypted_api_token", nullable = false)
    private String encryptedApiToken;

    @Column(name = "client_source", length = 100)
    private String clientSource;

    @Column(name = "pick_name", nullable = false)
    private String pickName;

    @Column(name = "pick_tel", nullable = false, length = 20)
    private String pickTel;

    @Column(name = "pick_address", nullable = false)
    private String pickAddress;

    @Column(name = "pick_province", nullable = false, length = 100)
    private String pickProvince;

    @Column(name = "pick_district", nullable = false, length = 100)
    private String pickDistrict;

    @Column(name = "pick_ward", length = 100)
    private String pickWard;

    @Column(name = "pick_option", nullable = false, length = 20)
    private String pickOption = "cod";

    @Column(nullable = false, length = 20)
    private String transport = "road";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;
}
