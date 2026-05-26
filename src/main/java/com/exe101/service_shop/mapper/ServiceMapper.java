package com.exe101.service_shop.mapper;

import com.exe101.service_shop.dto.ServiceDetailDTO;
import com.exe101.service_shop.dto.ServiceWriteDTO;
import com.exe101.service_shop.entity.Service;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {
    public static ServiceDetailDTO toDetailDTO(Service e) {
        if (e == null) return null;

        ServiceDetailDTO dto = new ServiceDetailDTO();
        dto.setId(e.getId());
        dto.setShopId(e.getShopId());
        dto.setShopName(e.getShop() != null ? e.getShop().getName() : null);
        dto.setShopPhone(e.getShop() != null ? e.getShop().getPhone() : null);
        dto.setShopAddress(e.getShop() != null ? e.getShop().getAddressText() : null);
        dto.setShopImageUrl(e.getShop() != null ? e.getShop().getImageUrl() : null);
        dto.setShopLat(e.getShop() != null ? e.getShop().getLat() : null);
        dto.setShopLng(e.getShop() != null ? e.getShop().getLng() : null);
        dto.setName(e.getName());
        dto.setDurationMin(e.getDurationMin());
        dto.setBasePrice(e.getBasePrice());
        dto.setCategoryId(e.getCategoryId());
        dto.setCategoryName(e.getCategory() != null ? e.getCategory().getName() : null);
        dto.setServiceType(e.getServiceType());
        dto.setVeterinaryServiceType(e.getVeterinaryServiceType());
        dto.setVaccineId(e.getVaccineId());
        dto.setVaccineName(e.getVaccine() != null ? e.getVaccine().getName() : null);
        dto.setImageUrl(e.getImageUrl());
        dto.setActive(e.getActive());
        return dto;
    }

    public static Service toEntity(Long shopId, ServiceWriteDTO dto) {
        if (dto == null) return null;

        Service e = new Service();
        e.setShopId(shopId);
        e.setName(dto.getName());
        e.setDurationMin(dto.getDurationMin());
        e.setBasePrice(dto.getBasePrice());
        e.setCategoryId(dto.getCategoryId());
        e.setServiceType(dto.getServiceType());
        e.setVeterinaryServiceType(dto.getVeterinaryServiceType());
        e.setVaccineId(dto.getVaccineId());
        e.setImageUrl(dto.getImageUrl());
        e.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        return e;
    }
}
