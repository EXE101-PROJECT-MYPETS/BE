package com.exe101.servicePackage.mapper;

import com.exe101.servicePackage.dto.ServicePackageDTO;
import com.exe101.servicePackage.entity.ServicePackage;
import org.springframework.stereotype.Component;

@Component
public class ServicePackageMapper {

    public static ServicePackageDTO toDTO(ServicePackage entity) {
        if (entity == null) return null;
        return new ServicePackageDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getName(),
                entity.getPrice(),
                entity.getTotalUses(),
                entity.getExpiryDays(),
                entity.getActive()
        );
    }

    public static ServicePackage toEntity(ServicePackageDTO dto) {
        if (dto == null) return null;
        ServicePackage entity = new ServicePackage();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(ServicePackage entity, ServicePackageDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setName(dto.getName());
        entity.setPrice(dto.getPrice());
        entity.setTotalUses(dto.getTotalUses());
        entity.setExpiryDays(dto.getExpiryDays());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
    }
}
