package com.exe101.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategoryStatDTO {
    private String categoryId;
    private String categoryName;
    private Long serviceCount;
}
