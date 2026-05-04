package com.exe101.shopGhtkConfig.mapper;

import com.exe101.shopGhtkConfig.dto.ShopGhtkConfigDTO;
import com.exe101.shopGhtkConfig.entity.ShopGhtkConfig;
import org.springframework.stereotype.Component;

@Component
public class ShopGhtkConfigMapper {

    public static ShopGhtkConfigDTO toDTO(ShopGhtkConfig entity) {
        if (entity == null) return null;
        return new ShopGhtkConfigDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getEnabled(),
                null,
                entity.getEncryptedApiToken() != null && !entity.getEncryptedApiToken().isBlank(),
                entity.getClientSource(),
                entity.getPickName(),
                entity.getPickTel(),
                entity.getPickAddress(),
                entity.getPickProvince(),
                entity.getPickDistrict(),
                entity.getPickWard(),
                entity.getPickOption(),
                entity.getTransport(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static void updateEntity(ShopGhtkConfig entity, ShopGhtkConfigDTO dto) {
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        entity.setClientSource(trim(dto.getClientSource()));
        entity.setPickName(trim(dto.getPickName()));
        entity.setPickTel(trim(dto.getPickTel()));
        entity.setPickAddress(trim(dto.getPickAddress()));
        entity.setPickProvince(trim(dto.getPickProvince()));
        entity.setPickDistrict(trim(dto.getPickDistrict()));
        entity.setPickWard(trim(dto.getPickWard()));
        entity.setPickOption(defaultIfBlank(dto.getPickOption(), "cod"));
        entity.setTransport(defaultIfBlank(dto.getTransport(), "road"));
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? defaultValue : trimmed;
    }
}
