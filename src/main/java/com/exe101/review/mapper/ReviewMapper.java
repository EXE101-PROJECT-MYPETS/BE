package com.exe101.review.mapper;

import com.exe101.review.dto.ReviewDTO;
import com.exe101.review.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public static ReviewDTO toDTO(Review entity) {
        if (entity == null) return null;

        String customerName = entity.getCustomer() != null
                ? entity.getCustomer().getFullName()
                : null;

        return new ReviewDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getProductId(),
                entity.getCustomerId(),
                customerName,
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Review toEntity(ReviewDTO dto) {
        if (dto == null) return null;
        Review entity = new Review();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Review entity, ReviewDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setProductId(dto.getProductId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setRating(dto.getRating());
        entity.setComment(dto.getComment());
    }
}
