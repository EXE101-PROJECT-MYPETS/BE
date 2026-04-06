package com.exe101.inventory.mapper;

import com.exe101.inventory.dto.InventoryDTO;
import com.exe101.inventory.entity.Inventory;
import com.exe101.inventory.entity.InventoryId;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public static InventoryDTO toDTO(Inventory entity) {
        if (entity == null) return null;
        return new InventoryDTO(
                entity.getShopId(),
                entity.getProductId(),
                entity.getOnHand(),
                entity.getReserved(),
                entity.getUpdatedAt()
        );
    }

    public static Inventory toEntity(InventoryDTO dto) {
        if (dto == null) return null;
        Inventory entity = new Inventory();
        entity.setId(new InventoryId(dto.getShopId(), dto.getProductId()));
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Inventory entity, InventoryDTO dto) {
        if (entity.getId() == null) {
            entity.setId(new InventoryId(dto.getShopId(), dto.getProductId()));
        }
        entity.setOnHand(dto.getOnHand() != null ? dto.getOnHand() : 0L);
        entity.setReserved(dto.getReserved() != null ? dto.getReserved() : 0L);
    }
}
