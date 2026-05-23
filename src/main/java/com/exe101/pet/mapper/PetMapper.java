package com.exe101.pet.mapper;

import com.exe101.pet.dto.PetDTO;
import com.exe101.pet.entity.Pet;
import org.springframework.stereotype.Component;

@Component
public class PetMapper {

    public static PetDTO toDTO(Pet entity) {
        if (entity == null) return null;
        return new PetDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getSpeciesId(),
                entity.getBreedId(),
                entity.getBreedText(),
                entity.getAvatarUrl(),
                entity.getName(),
                entity.getGender(),
                entity.getDob(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null
        );
    }

    public static Pet toEntity(PetDTO dto) {
        if (dto == null) return null;
        Pet entity = new Pet();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Pet entity, PetDTO dto) {
        entity.setUserId(dto.getUserId());
        entity.setSpeciesId(dto.getSpeciesId());
        entity.setBreedId(dto.getBreedId());
        entity.setBreedText(dto.getBreedText());
        entity.setAvatarUrl(dto.getAvatarUrl());
        entity.setName(dto.getName());
        entity.setGender(dto.getGender());
        entity.setDob(dto.getDob());
        entity.setNote(dto.getNote());
    }
}
