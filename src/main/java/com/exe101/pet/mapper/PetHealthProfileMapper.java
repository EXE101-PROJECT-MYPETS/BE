package com.exe101.pet.mapper;

import com.exe101.pet.dto.PetHealthProfileDTO;
import com.exe101.pet.entity.PetHealthProfile;
import org.springframework.stereotype.Component;

@Component
public class PetHealthProfileMapper {

    public static PetHealthProfileDTO toDTO(PetHealthProfile entity) {
        if (entity == null) return null;
        return new PetHealthProfileDTO(
                entity.getPetId(),
                entity.getAllergies(),
                entity.getConditions(),
                entity.getNotes(),
                entity.getUpdatedAt()
        );
    }

    public static PetHealthProfile toEntity(PetHealthProfileDTO dto) {
        if (dto == null) return null;
        PetHealthProfile entity = new PetHealthProfile();
        entity.setPetId(dto.getPetId());
        entity.setAllergies(dto.getAllergies());
        entity.setConditions(dto.getConditions());
        entity.setNotes(dto.getNotes());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
}
