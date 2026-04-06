package com.exe101.pet.mapper;

import com.exe101.pet.dto.PetSpeciesDTO;
import com.exe101.pet.entity.PetSpecies;
import org.springframework.stereotype.Component;

@Component
public class PetSpeciesMapper {

    public static PetSpeciesDTO toDTO(PetSpecies entity) {
        if (entity == null) return null;
        return new PetSpeciesDTO(entity.getId(), entity.getName());
    }

    public static PetSpecies toEntity(PetSpeciesDTO dto) {
        if (dto == null) return null;
        PetSpecies entity = new PetSpecies();
        entity.setName(dto.getName());
        return entity;
    }
}
