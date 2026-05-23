package com.exe101.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertDashboardDTO {
    private InventoryAlertSummaryDTO summary;
    private List<InventoryAlertItemDTO> items;
}
