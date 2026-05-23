package com.exe101.booking.mapper;

import com.exe101.booking.dto.BookingDTO;
import com.exe101.booking.entity.Booking;
import com.exe101.booking.entity.BookingSource;
import com.exe101.booking.entity.BookingStatus;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public static BookingDTO toDTO(Booking entity) {
        if (entity == null) return null;
        return new BookingDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getUserId(),
                entity.getCustomerId(),
                entity.getPetId(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getStatus(),
                entity.getSource(),
                entity.getNote(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Booking toEntity(BookingDTO dto) {
        if (dto == null) return null;
        Booking entity = new Booking();
        updateEntity(entity, dto);
        return entity;
    }

    public static void updateEntity(Booking entity, BookingDTO dto) {
        entity.setShopId(dto.getShopId());
        entity.setUserId(dto.getUserId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setPetId(dto.getPetId());
        entity.setStartAt(dto.getStartAt());
        entity.setEndAt(dto.getEndAt());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : BookingStatus.DRAFT);
        entity.setSource(dto.getSource() != null ? dto.getSource() : BookingSource.STAFF);
        entity.setNote(dto.getNote());
        entity.setCreatedBy(dto.getCreatedBy());
    }
}
