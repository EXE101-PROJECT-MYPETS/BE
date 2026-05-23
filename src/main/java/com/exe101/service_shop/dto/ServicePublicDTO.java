package com.exe101.service_shop.dto;

import com.exe101.service_shop.entity.ServiceType;
import com.exe101.service_shop.entity.VeterinaryServiceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicePublicDTO {
    private ServiceInfoDTO service;
    private ShopInfoDTO shop;
    private Double distanceKm;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceInfoDTO {
        private Long id;
        private String name;
        private Integer durationMin;
        private Long basePrice;
        private Long categoryId;
        private ServiceType serviceType;
        private VeterinaryServiceType veterinaryServiceType;
        private Long vaccineId;
        private String vaccineName;
        private String imageUrl;
        private Boolean active;
        private Double rating;
        private Long ratingCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopInfoDTO {
        private Long shopId;
        private String shopName;
        private String shopImageUrl;
        private String shopAddress;
        private String shopProvince;
        private Double shopLat;
        private Double shopLng;
    }
}
