package com.exe101.pet.entity;

import com.exe101.customer.entity.Customer;
import com.exe101.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "species_id", nullable = false)
    private Long speciesId;

    @Column(name = "breed_id")
    private Long breedId;

    @Column(name = "breed_text")
    private String breedText;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private String name;

    private String gender;

    private LocalDate dob;

    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "shop_id", referencedColumnName = "shop_id", insertable = false, updatable = false),
            @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    @JsonIgnore
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", insertable = false, updatable = false)
    @JsonIgnore
    private PetSpecies species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", insertable = false, updatable = false)
    @JsonIgnore
    private PetBreed breed;
}
