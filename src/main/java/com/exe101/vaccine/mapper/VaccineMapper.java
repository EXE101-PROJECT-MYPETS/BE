package com.exe101.vaccine.mapper;

import com.exe101.vaccine.dto.VaccineDTO;
import com.exe101.vaccine.entity.Vaccine;
import org.springframework.stereotype.Component;

@Component
public class VaccineMapper {

    public static VaccineDTO toDTO(Vaccine entity) {
        if (entity == null) return null;
        return new VaccineDTO(
                entity.getId(),
                entity.getSpeciesId(),
                entity.getName(),
                entity.getDescription(),
                entity.getBoosterIntervalDays(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Vaccine toEntity(VaccineDTO dto) {
        if (dto == null) return null;
        Vaccine entity = new Vaccine();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Vaccine entity, VaccineDTO dto) {
        entity.setSpeciesId(dto.getSpeciesId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setBoosterIntervalDays(dto.getBoosterIntervalDays());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
    }
}
