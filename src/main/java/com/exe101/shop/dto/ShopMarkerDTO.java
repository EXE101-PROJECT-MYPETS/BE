package com.exe101.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopMarkerDTO {
    private Long id;
    private String name;
    private Double lat;
    private Double lng;
    private String address;
    private String imageUrl;
    private Double rating;
}
