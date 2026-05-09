package com.exe101.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopPublicDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private Double rating;
    private Long productCount;
    private List<String> badges;
    private String address;
    private ShopPublicContactDTO contact;
}
