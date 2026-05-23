package com.exe101.pet.entity;

import com.exe101.customer.entity.Customer;
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

    @Column(name = "user_id")
    private Long userId;


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
    @JoinColumn(name = "species_id", insertable = false, updatable = false)
    @JsonIgnore
    private PetSpecies species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", insertable = false, updatable = false)
    @JsonIgnore
    private PetBreed breed;
}
