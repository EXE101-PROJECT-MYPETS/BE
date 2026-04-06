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
@Table(name = "inventory")
public class Inventory {

    @EmbeddedId
    private InventoryId id;

    @Column(name = "shop_id", nullable = false, insertable = false, updatable = false)
    private Long shopId;

    @Column(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Long productId;

    @Column(name = "on_hand", nullable = false)
    private Long onHand = 0L;

    @Column(nullable = false)
    private Long reserved = 0L;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

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

    @PrePersist
    public void prePersist() {
        if (id != null) {
            this.shopId = id.getShopId();
            this.productId = id.getProductId();
        }
    }
}
