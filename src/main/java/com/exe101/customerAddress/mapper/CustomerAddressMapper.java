package com.exe101.customerAddress.mapper;

import com.exe101.customerAddress.dto.CustomerAddressDTO;
import com.exe101.customerAddress.entity.CustomerAddress;

public class CustomerAddressMapper {

    private CustomerAddressMapper() {
    }

    public static CustomerAddressDTO toDTO(CustomerAddress entity) {
        if (entity == null) return null;
        return new CustomerAddressDTO(
                entity.getId(),
                entity.getCustomerId(),
                entity.getName(),
                entity.getTel(),
                entity.getAddress(),
                entity.getProvince(),
                entity.getDistrict(),
                entity.getWard(),
                entity.getHamlet(),
                entity.getDefaultAddress(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static CustomerAddress toEntity(CustomerAddressDTO dto) {
        if (dto == null) return null;
        CustomerAddress entity = new CustomerAddress();
        entity.setCustomerId(dto.getCustomerId());
        entity.setName(trim(dto.getName()));
        entity.setTel(trim(dto.getTel()));
        entity.setAddress(trim(dto.getAddress()));
        entity.setProvince(trim(dto.getProvince()));
        entity.setDistrict(trim(dto.getDistrict()));
        entity.setWard(trim(dto.getWard()));
        entity.setHamlet(trim(dto.getHamlet()));
        entity.setDefaultAddress(Boolean.TRUE.equals(dto.getDefaultAddress()));
        return entity;
    }

    public static void updateEntity(CustomerAddress entity, CustomerAddressDTO dto) {
        entity.setName(trim(dto.getName()));
        entity.setTel(trim(dto.getTel()));
        entity.setAddress(trim(dto.getAddress()));
        entity.setProvince(trim(dto.getProvince()));
        entity.setDistrict(trim(dto.getDistrict()));
        entity.setWard(trim(dto.getWard()));
        entity.setHamlet(trim(dto.getHamlet()));
        entity.setDefaultAddress(Boolean.TRUE.equals(dto.getDefaultAddress()));
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
