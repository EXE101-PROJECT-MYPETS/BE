package com.exe101.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopNearbyDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String coverImageUrl;
    private Double rating;
    private Long productCount;
    private Long serviceCount;
    private String address;
    private Double lat;
    private Double lng;
    private Double distanceKm;
    private String openingHours;
    private String closingHours;
}
