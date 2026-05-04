package com.exe101.shopPaymentConfig.mapper;

import com.exe101.shopPaymentConfig.dto.ShopPaymentConfigDTO;
import com.exe101.shopPaymentConfig.entity.ShopPaymentConfig;
import org.springframework.stereotype.Component;

@Component
public class ShopPaymentConfigMapper {

    public static ShopPaymentConfigDTO toDTO(ShopPaymentConfig entity) {
        if (entity == null) return null;
        return new ShopPaymentConfigDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getBankCode(),
                entity.getAccountNumber(),
                entity.getAccountName(),
                entity.getDisplayName(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ShopPaymentConfig toEntity(ShopPaymentConfigDTO dto) {
        if (dto == null) return null;
        ShopPaymentConfig entity = new ShopPaymentConfig();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(ShopPaymentConfig entity, ShopPaymentConfigDTO dto) {
        entity.setBankCode(trim(dto.getBankCode()));
        entity.setAccountNumber(trim(dto.getAccountNumber()));
        entity.setAccountName(trim(dto.getAccountName()));
        entity.setDisplayName(trim(dto.getDisplayName()));
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
