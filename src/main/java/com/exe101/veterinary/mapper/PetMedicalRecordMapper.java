package com.exe101.veterinary.mapper;

import com.exe101.veterinary.dto.PetMedicalRecordDTO;
import com.exe101.veterinary.entity.PetMedicalRecord;

public class PetMedicalRecordMapper {

    private PetMedicalRecordMapper() {
    }

    public static PetMedicalRecordDTO toDTO(PetMedicalRecord entity) {
        if (entity == null) {
            return null;
        }
        return new PetMedicalRecordDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getPetId(),
                entity.getBookingId(),
                entity.getBookingItemId(),
                entity.getServiceId(),
                entity.getService() != null ? entity.getService().getName() : null,
                entity.getVaccineId(),
                entity.getVaccine() != null ? entity.getVaccine().getName() : null,
                entity.getVeterinarianUserId(),
                entity.getRecordType(),
                entity.getVeterinaryServiceType(),
                entity.getPerformedAt(),
                entity.getSymptoms(),
                entity.getDiagnosis(),
                entity.getTreatment(),
                entity.getNote(),
                entity.getFollowUpAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
