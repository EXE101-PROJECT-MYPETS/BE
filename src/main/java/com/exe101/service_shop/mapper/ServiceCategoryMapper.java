package com.exe101.service_shop.mapper;

import com.exe101.service_shop.dto.ServiceCategoryDTO;
import com.exe101.service_shop.entity.ServiceCategory;
import org.springframework.stereotype.Component;

@Component
public class ServiceCategoryMapper {

    public static ServiceCategoryDTO toDTO(ServiceCategory entity) {
        if (entity == null) return null;

        return new ServiceCategoryDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getName(),
                entity.getDescription(),
                entity.getActive(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ServiceCategory toEntity(ServiceCategoryDTO dto) {
        if (dto == null) return null;

        ServiceCategory entity = new ServiceCategory();
        entity.setShopId(dto.getShopId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return entity;
    }

    public static void updateEntity(ServiceCategory entity, ServiceCategoryDTO dto) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
    }
}
