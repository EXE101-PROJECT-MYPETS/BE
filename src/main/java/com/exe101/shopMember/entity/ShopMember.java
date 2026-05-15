package com.exe101.shopMember.entity;

import com.exe101.shop.entity.Shop;
import com.exe101.shop.entity.ShopRole;
import com.exe101.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "shop_members")
public class ShopMember {

    @EmbeddedId
    private ShopMemberId id;

    // Giữ scalar để query/filter dễ và rõ multi-tenant
    @Column(name = "shop_id", nullable = false, insertable = false, updatable = false)
    private Long shopId;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    // Quan hệ chỉ để đọc (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ShopRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = MemberStatus.ACTIVE;
        if (id != null) {
            this.shopId = id.getShopId();
            this.userId = id.getUserId();
        }
    }
}
