package com.exe101.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDTO {
    private Long shopId;
    private Long productId;
    private Long onHand;
    private Long reserved;
    private OffsetDateTime updatedAt;
}
