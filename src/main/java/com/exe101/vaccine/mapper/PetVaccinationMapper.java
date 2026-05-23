package com.exe101.vaccine.mapper;

import com.exe101.vaccine.dto.PetVaccinationDTO;
import com.exe101.vaccine.entity.PetVaccination;
import org.springframework.stereotype.Component;

@Component
public class PetVaccinationMapper {

    public static PetVaccinationDTO toDTO(PetVaccination entity) {
        if (entity == null) return null;
        return new PetVaccinationDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getPetId(),
                entity.getVaccineId(),
                entity.getVaccine() != null ? entity.getVaccine().getName() : null,
                entity.getBookingId(),
                entity.getBookingItemId(),
                entity.getServiceId(),
                entity.getMedicalRecordId(),
                entity.getVaccinatedAt(),
                entity.getNextDueAt(),
                entity.getClinicName(),
                entity.getVetName(),
                entity.getBatchNo(),
                entity.getNote(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    public static PetVaccination toEntity(PetVaccinationDTO dto) {
        if (dto == null) return null;
        PetVaccination entity = new PetVaccination();
        entity.setShopId(dto.getShopId());
        entity.setPetId(dto.getPetId());
        entity.setVaccineId(dto.getVaccineId());
        entity.setBookingId(dto.getBookingId());
        entity.setBookingItemId(dto.getBookingItemId());
        entity.setServiceId(dto.getServiceId());
        entity.setMedicalRecordId(dto.getMedicalRecordId());
        entity.setVaccinatedAt(dto.getVaccinatedAt());
        entity.setNextDueAt(dto.getNextDueAt());
        entity.setClinicName(dto.getClinicName());
        entity.setVetName(dto.getVetName());
        entity.setBatchNo(dto.getBatchNo());
        entity.setNote(dto.getNote());
        entity.setCreatedBy(dto.getCreatedBy());
        return entity;
    }
}
