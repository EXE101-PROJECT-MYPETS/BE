package com.exe101.booking.mapper;

import com.exe101.booking.dto.BookingStatusEventDTO;
import com.exe101.booking.entity.BookingStatusEvent;
import org.springframework.stereotype.Component;

@Component
public class BookingStatusEventMapper {

    public static BookingStatusEventDTO toDTO(BookingStatusEvent entity) {
        if (entity == null) return null;
        return new BookingStatusEventDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getBookingId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getActorUserId(),
                entity.getMetaJson(),
                entity.getCreatedAt()
        );
    }

    public static BookingStatusEvent toEntity(BookingStatusEventDTO dto) {
        if (dto == null) return null;
        BookingStatusEvent entity = new BookingStatusEvent();
        entity.setShopId(dto.getShopId());
        entity.setBookingId(dto.getBookingId());
        entity.setFromStatus(dto.getFromStatus());
        entity.setToStatus(dto.getToStatus());
        entity.setActorUserId(dto.getActorUserId());
        entity.setMetaJson(dto.getMetaJson());
        return entity;
    }
}
