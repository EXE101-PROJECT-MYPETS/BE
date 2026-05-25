package com.exe101.userAddress.mapper;

import com.exe101.userAddress.dto.UserAddressDTO;
import com.exe101.userAddress.entity.UserAddress;

public class UserAddressMapper {

    private UserAddressMapper() {
    }

    public static UserAddressDTO toDTO(UserAddress entity) {
        if (entity == null) return null;
        return new UserAddressDTO(
                entity.getId(),
                entity.getUserId(),
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

    public static UserAddress toEntity(UserAddressDTO dto) {
        if (dto == null) return null;
        UserAddress entity = new UserAddress();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(UserAddress entity, UserAddressDTO dto) {
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
