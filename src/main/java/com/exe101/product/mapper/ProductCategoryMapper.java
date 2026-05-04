package com.exe101.product.mapper;

import com.exe101.product.dto.ProductCategoryDTO;
import com.exe101.product.entity.ProductCategory;
import org.springframework.stereotype.Component;

@Component
public class ProductCategoryMapper {

    public static ProductCategoryDTO toDTO(ProductCategory entity) {
        if (entity == null) return null;

        return new ProductCategoryDTO(
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

    public static ProductCategory toEntity(ProductCategoryDTO dto) {
        if (dto == null) return null;

        ProductCategory entity = new ProductCategory();
        entity.setShopId(dto.getShopId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return entity;
    }

    public static void updateEntity(ProductCategory entity, ProductCategoryDTO dto) {
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
