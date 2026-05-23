package com.exe101.veterinary.entity;

import com.exe101.pet.entity.Pet;
import com.exe101.service_shop.entity.Service;
import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.entity.VeterinaryServiceType;
import com.exe101.user.entity.User;
import com.exe101.vaccine.entity.Vaccine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "pet_medical_records")
public class PetMedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "booking_item_id")
    private Long bookingItemId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "vaccine_id")
    private Long vaccineId;

    @Column(name = "veterinarian_user_id")
    private Long veterinarianUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 50)
    private ServiceType recordType = ServiceType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "veterinary_service_type", length = 50)
    private VeterinaryServiceType veterinaryServiceType;

    @Column(name = "performed_at", nullable = false)
    private OffsetDateTime performedAt;

    @Column(columnDefinition = "text")
    private String symptoms;

    @Column(columnDefinition = "text")
    private String diagnosis;

    @Column(columnDefinition = "text")
    private String treatment;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "follow_up_at")
    private OffsetDateTime followUpAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    @JsonIgnore
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    @JsonIgnore
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", insertable = false, updatable = false)
    @JsonIgnore
    private Vaccine vaccine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User veterinarian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnore
    private User createdByUser;
}
