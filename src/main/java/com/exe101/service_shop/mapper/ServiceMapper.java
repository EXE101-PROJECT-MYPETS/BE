package com.exe101.service_shop.mapper;

import com.exe101.service_shop.dto.ServiceDTO;
import com.exe101.service_shop.entity.Service;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {
    public static ServiceDTO toDTO(Service e) {
        if (e == null) return null;

        ServiceDTO dto = new ServiceDTO();
        dto.setId(e.getId());
        dto.setShopId(e.getShopId());
        dto.setName(e.getName());
        dto.setDurationMin(e.getDurationMin());
        dto.setBasePrice(e.getBasePrice());
        dto.setCategoryId(e.getCategoryId());
        dto.setImageUrl(e.getImageUrl());
        dto.setActive(e.getActive());
        return dto;
    }

    public static Service toEntity(ServiceDTO dto) {
        if (dto == null) return null;

        Service e = new Service();
        e.setShopId(dto.getShopId());
        e.setName(dto.getName());
        e.setDurationMin(dto.getDurationMin());
        e.setBasePrice(dto.getBasePrice());
        e.setCategoryId(dto.getCategoryId());
        e.setImageUrl(dto.getImageUrl());
        e.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        return e;
    }
}
