package com.exe101.servicePackage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageDTO {
    private Long id;
    private Long shopId;
    private String name;
    private Long price;
    private Integer totalUses;
    private Integer expiryDays;
    private Boolean active;
}
