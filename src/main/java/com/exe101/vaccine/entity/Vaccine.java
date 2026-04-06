package com.exe101.vaccine.entity;

import com.exe101.pet.entity.PetSpecies;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "vaccines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_vaccines_species_name", columnNames = {"species_id", "name"})
        }
)
public class Vaccine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "species_id", nullable = false)
    private Long speciesId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "booster_interval_days")
    private Integer boosterIntervalDays;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", insertable = false, updatable = false)
    @JsonIgnore
    private PetSpecies species;
}
