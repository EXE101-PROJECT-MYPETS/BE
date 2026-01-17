package com.exe101.shopMember.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class ShopMemberId implements Serializable {

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "user_id")
    private Long userId;

    public ShopMemberId() {}

    public ShopMemberId(Long shopId, Long userId) {
        this.shopId = shopId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShopMemberId that)) return false;
        return Objects.equals(shopId, that.shopId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shopId, userId);
    }
}
