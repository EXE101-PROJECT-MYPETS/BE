package com.exe101.pet.mapper;

import com.exe101.pet.dto.PetBreedDTO;
import com.exe101.pet.entity.PetBreed;
import org.springframework.stereotype.Component;

@Component
public class PetBreedMapper {

    public static PetBreedDTO toDTO(PetBreed entity) {
        if (entity == null) return null;
        return new PetBreedDTO(entity.getId(), entity.getSpeciesId(), entity.getName());
    }

    public static PetBreed toEntity(PetBreedDTO dto) {
        if (dto == null) return null;
        PetBreed entity = new PetBreed();
        entity.setSpeciesId(dto.getSpeciesId());
        entity.setName(dto.getName());
        return entity;
    }
}
