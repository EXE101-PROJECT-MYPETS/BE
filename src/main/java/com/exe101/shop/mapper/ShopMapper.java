package com.exe101.shop.mapper;

import com.exe101.shop.dto.ShopDTO;
import com.exe101.shop.entity.LocationSource;
import com.exe101.shop.entity.Shop;
import com.exe101.shop.entity.ShopStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class ShopMapper {

    public static ShopDTO toDTO(Shop entity) {
        if (entity == null) return null;
        ShopDTO dto = new ShopDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAddressText(entity.getAddressText());
        dto.setImageUrl(entity.getImageUrl());
        dto.setLat(entity.getLat());
        dto.setLng(entity.getLng());
        dto.setLocationSource(entity.getLocationSource());
        dto.setLocationAccuracyM(entity.getLocationAccuracyM());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public static Shop toEntity(ShopDTO req) {
        if (req == null) return null;
        Shop entity = new Shop();
        entity.setName(req.getName());
        entity.setAddressText(req.getAddressText());
        entity.setImageUrl(req.getImageUrl());
        entity.setLat(req.getLat());
        entity.setLng(req.getLng());
        entity.setLocationSource(
                req.getLocationSource() != null
                        ? req.getLocationSource()
                        : LocationSource.MANUAL
        );
        entity.setLocationAccuracyM(req.getLocationAccuracyM());
        entity.setStatus(ShopStatus.ACTIVE);
        entity.setLocationUpdatedAt(OffsetDateTime.now());

        return entity;
    }
}
