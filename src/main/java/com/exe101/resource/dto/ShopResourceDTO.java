package com.exe101.resource.dto;

import com.exe101.resource.entity.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopResourceDTO {
    private Long id;
    private Long shopId;
    private ResourceType type;
    private String name;
    private Boolean active;
}
