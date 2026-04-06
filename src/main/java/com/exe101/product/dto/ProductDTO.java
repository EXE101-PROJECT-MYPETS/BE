package com.exe101.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private Long shopId;
    private String sku;
    private String name;
    private String unit;
    private Long price;
    private Boolean active;
}
