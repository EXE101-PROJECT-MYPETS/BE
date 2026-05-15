package com.exe101.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchItemDTO {
    private Long id;
    private SearchItemType type;
    private String name;
    private String image;
    private Long price;
    private Long originalPrice;
    private Long shopId;
    private String shopName;
    private Double rating;
    private Long soldCount;
    private String address;
    private Double distanceKm;
}

