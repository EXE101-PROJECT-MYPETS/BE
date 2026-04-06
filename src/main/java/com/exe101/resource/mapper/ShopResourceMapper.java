package com.exe101.resource.mapper;

import com.exe101.resource.dto.ShopResourceDTO;
import com.exe101.resource.entity.ShopResource;
import org.springframework.stereotype.Component;

@Component
public class ShopResourceMapper {

    public static ShopResourceDTO toDTO(ShopResource entity) {
        if (entity == null) return null;
        return new ShopResourceDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getType(),
                entity.getName(),
                entity.getActive()
        );
    }

    public static ShopResource toEntity(ShopResourceDTO dto) {
        if (dto == null) return null;
        ShopResource entity = new ShopResource();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(ShopResource entity, ShopResourceDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setType(dto.getType());
        entity.setName(dto.getName());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
    }
}
