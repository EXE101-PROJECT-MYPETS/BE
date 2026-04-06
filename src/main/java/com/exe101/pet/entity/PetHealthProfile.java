package com.exe101.pet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "pet_health_profiles")
public class PetHealthProfile {

    @Id
    @Column(name = "pet_id")
    private Long petId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JsonIgnore
    @JoinColumn(name = "pet_id")
    private Pet pet;

    private String allergies;

    private String conditions;

    private String notes;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }
}
