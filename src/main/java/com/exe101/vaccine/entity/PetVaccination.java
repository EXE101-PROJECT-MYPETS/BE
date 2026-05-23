package com.exe101.vaccine.entity;

import com.exe101.pet.entity.Pet;
import com.exe101.shop.entity.Shop;
import com.exe101.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "pet_vaccinations")
public class PetVaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "vaccine_id", nullable = false)
    private Long vaccineId;

    @Column(name = "vaccinated_at", nullable = false)
    private LocalDate vaccinatedAt;

    @Column(name = "next_due_at")
    private LocalDate nextDueAt;

    @Column(name = "clinic_name")
    private String clinicName;

    @Column(name = "vet_name")
    private String vetName;

    @Column(name = "batch_no")
    private String batchNo;

    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    @JsonIgnore
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "pet_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", insertable = false, updatable = false)
    @JsonIgnore
    private Vaccine vaccine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnore
    private User createdByUser;
}
