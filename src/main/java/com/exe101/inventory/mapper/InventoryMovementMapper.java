package com.exe101.inventory.mapper;

import com.exe101.inventory.dto.InventoryMovementDTO;
import com.exe101.inventory.entity.InventoryMovement;
import org.springframework.stereotype.Component;

@Component
public class InventoryMovementMapper {

    public static InventoryMovementDTO toDTO(InventoryMovement entity) {
        if (entity == null) return null;
        return new InventoryMovementDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getProductId(),
                entity.getQtyDelta(),
                entity.getReason(),
                entity.getRefType(),
                entity.getRefId(),
                entity.getCreatedAt()
        );
    }

    public static InventoryMovement toEntity(InventoryMovementDTO dto) {
        if (dto == null) return null;
        InventoryMovement entity = new InventoryMovement();
        entity.setShopId(dto.getShopId());
        entity.setProductId(dto.getProductId());
        entity.setQtyDelta(dto.getQtyDelta());
        entity.setReason(dto.getReason());
        entity.setRefType(dto.getRefType());
        entity.setRefId(dto.getRefId());
        return entity;
    }
}
