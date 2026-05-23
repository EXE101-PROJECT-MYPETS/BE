package com.exe101.pet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "pet_shop_links")
@IdClass(PetShopLink.PetShopLinkId.class)
public class PetShopLink {

    @Id
    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Id
    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "linked_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime linkedAt;

    @Getter
    @Setter
    public static class PetShopLinkId implements Serializable {
        private Long petId;
        private Long shopId;

        public PetShopLinkId() {
        }

        public PetShopLinkId(Long petId, Long shopId) {
            this.petId = petId;
            this.shopId = shopId;
        }
    }
}
