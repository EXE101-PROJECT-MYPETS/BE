package com.exe101.servicePackage.mapper;

import com.exe101.servicePackage.dto.CustomerPackageDTO;
import com.exe101.servicePackage.entity.CustomerPackage;
import org.springframework.stereotype.Component;

@Component
public class CustomerPackageMapper {

    public static CustomerPackageDTO toDTO(CustomerPackage entity) {
        if (entity == null) return null;
        return new CustomerPackageDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getCustomerId(),
                entity.getPackageId(),
                entity.getPurchasedAt(),
                entity.getExpiresAt(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public static CustomerPackage toEntity(CustomerPackageDTO dto) {
        if (dto == null) return null;
        CustomerPackage entity = new CustomerPackage();
        entity.setShopId(dto.getShopId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setPackageId(dto.getPackageId());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        return entity;
    }
}
