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
public class ServiceDetailDTO {

    private Long id;

    private Long shopId;
    private String shopName;
    private String shopPhone;
    private String shopAddress;
    private String shopImageUrl;
    private Double shopLat;
    private Double shopLng;
    private Double distanceKm;

    private String name;
    private Integer durationMin;
    private Long basePrice;

    private Long categoryId;
    private String categoryName;

    private ServiceType serviceType;
    private VeterinaryServiceType veterinaryServiceType;

    private Long vaccineId;
    private String vaccineName;

    private String imageUrl;
    private Boolean active;
}
