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
}
