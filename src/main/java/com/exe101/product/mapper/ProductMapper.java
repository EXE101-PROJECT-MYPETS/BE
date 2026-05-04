package com.exe101.product.mapper;

import com.exe101.product.dto.ProductDTO;
import com.exe101.product.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductMapper {

    public static ProductDTO toDTO(Product entity) {
        if (entity == null) return null;
        return new ProductDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getCategoryId(),
                null,
                entity.getSku(),
                entity.getName(),
                entity.getUnit(),
                entity.getPrice(),
                entity.getWeightKg(),
                entity.getActive(),
                null,
                null
        );
    }

    public static Product toEntity(ProductDTO dto) {
        if (dto == null) return null;
        Product entity = new Product();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Product entity, ProductDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setCategoryId(dto.getCategoryId());
        entity.setSku(dto.getSku());
        entity.setName(dto.getName());
        entity.setUnit(dto.getUnit());
        entity.setPrice(dto.getPrice() != null ? dto.getPrice() : 0L);
        entity.setWeightKg(dto.getWeightKg() != null ? dto.getWeightKg() : new BigDecimal("0.100"));
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
    }
}
