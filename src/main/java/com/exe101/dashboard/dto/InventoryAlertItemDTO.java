package com.exe101.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertItemDTO {
    private String itemId;
    private String itemType;
    private String itemName;
    private String imageUrl;
    private String sku;
    private Long onHand;
    private Long reserved;
    private Long available;
    private Long reorderPoint;
    private String status;
}
