package com.exe101.pet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "pet_breeds",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pet_breeds_species_name", columnNames = {"species_id", "name"})
        }
)
public class PetBreed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "species_id", nullable = false)
    private Long speciesId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", insertable = false, updatable = false)
    @JsonIgnore
    private PetSpecies species;
}
