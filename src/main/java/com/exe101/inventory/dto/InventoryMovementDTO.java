package com.exe101.inventory.dto;

import com.exe101.inventory.entity.InventoryReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovementDTO {
    private Long id;
    private Long shopId;
    private Long productId;
    private Long qtyDelta;
    private InventoryReason reason;
    private String refType;
    private Long refId;
    private OffsetDateTime createdAt;
}
