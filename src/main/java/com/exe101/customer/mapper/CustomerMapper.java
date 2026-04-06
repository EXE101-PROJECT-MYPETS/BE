package com.exe101.customer.mapper;

import com.exe101.customer.dto.CustomerDTO;
import com.exe101.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public static CustomerDTO toDTO(Customer entity) {
        if (entity == null) return null;
        return new CustomerDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getUserId(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Customer toEntity(CustomerDTO dto) {
        if (dto == null) return null;
        Customer entity = new Customer();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Customer entity, CustomerDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setUserId(dto.getUserId());
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
    }
}
