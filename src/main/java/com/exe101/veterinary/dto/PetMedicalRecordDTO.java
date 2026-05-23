package com.exe101.veterinary.dto;

import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.entity.VeterinaryServiceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetMedicalRecordDTO {
    private Long id;
    private Long shopId;
    private Long petId;
    private Long bookingId;
    private Long bookingItemId;
    private Long serviceId;
    private String serviceName;
    private Long vaccineId;
    private String vaccineName;
    private Long veterinarianUserId;
    private ServiceType recordType;
    private VeterinaryServiceType veterinaryServiceType;
    private OffsetDateTime performedAt;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String note;
    private OffsetDateTime followUpAt;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
