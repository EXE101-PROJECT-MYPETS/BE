package com.exe101.serviceReview.mapper;

import com.exe101.serviceReview.dto.ServiceReviewDTO;
import com.exe101.serviceReview.entity.ServiceReview;
import org.springframework.stereotype.Component;

@Component
public class ServiceReviewMapper {

    public static ServiceReviewDTO toDTO(ServiceReview entity) {
        if (entity == null) return null;

        String customerName = entity.getCustomer() != null
                ? entity.getCustomer().getFullName()
                : null;

        ServiceReviewDTO dto = new ServiceReviewDTO();
        dto.setId(entity.getId());
        dto.setShopId(entity.getShopId());
        dto.setServiceId(entity.getServiceId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerName(customerName);
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setReply(entity.getReply());
        dto.setReplyAt(entity.getReplyAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static ServiceReview toEntity(ServiceReviewDTO dto) {
        if (dto == null) return null;
        ServiceReview entity = new ServiceReview();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(ServiceReview entity, ServiceReviewDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setServiceId(dto.getServiceId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setRating(dto.getRating());
        entity.setComment(dto.getComment());
        entity.setReply(dto.getReply());
        entity.setReplyAt(dto.getReplyAt());
    }
}
